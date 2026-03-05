package og.portal.zarplata.service;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import og.portal.zarplata.dto.SalaryMonthDTO;
import og.portal.zarplata.dto.SalaryRowDTO;
import og.portal.zarplata.model.ColorMapping;
import og.portal.zarplata.model.SalaryColumnMapping;
import og.portal.zarplata.model.SalaryParseSetting;
import og.portal.zarplata.repository.ColorMappingRepository;
import og.portal.zarplata.repository.SalaryColumnMappingRepository;
import og.portal.zarplata.repository.SalaryParseSettingRepository;
import og.portal.zarplata.service.excel.ExcelHelperService;
import og.portal.zarplata.service.util.DataCleaningService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalaryService {

    private static final String EXCEL_EXTENSION = ".xlsx";
    private static final Pattern MONTH_HEADER_PATTERN = Pattern.compile("^\\s*([А-ЯЁA-Z]+)\\s+(20\\d{2})", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);

    @Value("${app.share.folder.path}")
    private String shareFolderPath;

    @Value("${app.salary.folder.path}")
    private String salaryFolderPath;

    private final SalaryColumnMappingRepository columnMappingRepository;
    private final ColorMappingRepository colorMappingRepository;
    private final SalaryParseSettingRepository salaryParseSettingRepository;

    @SuppressWarnings("unchecked")
    public List<SalaryMonthDTO> getSalaryData(String username, HttpSession session) {
        String cacheKey = "salary_cache_" + username;
        List<SalaryMonthDTO> cached = (List<SalaryMonthDTO>) session.getAttribute(cacheKey);
        if (cached != null) {
            log.debug("SalaryService: Returning cached salary data for user '{}'", username);
            return cached;
        }

        log.info("SalaryService: Parsing salary file for user '{}'", username);
        List<SalaryMonthDTO> data = parseSalaryFile(username);
        session.setAttribute(cacheKey, data);
        log.debug("SalaryService: Cached salary data for user '{}' ({} months)", username, data.size());
        return data;
    }

    private List<SalaryMonthDTO> parseSalaryFile(String username) {
        File file = new File(shareFolderPath + salaryFolderPath, username + EXCEL_EXTENSION);
        if (!file.exists()) {
            log.warn("SalaryService: Salary file not found for user: {} (Path: {})", username, file.getAbsolutePath());
            return Collections.emptyList();
        }

        log.debug("SalaryService: Reading file: {}", file.getAbsolutePath());

        List<SalaryColumnMapping> mappings = columnMappingRepository.findAll();
        Map<String, String> colorMap = colorMappingRepository.findAll().stream()
                .collect(Collectors.toMap(ColorMapping::getExcelArgbHex, ColorMapping::getHtmlColorCode, (a, b) -> a));
        
        SalaryParseSetting settings = salaryParseSettingRepository.findAll().stream()
                .findFirst()
                .orElse(new SalaryParseSetting());

        int dateColIndex = settings.getDateColIndex();
        int startRow = settings.getStartRow();

        List<SalaryMonthDTO> result = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(file); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            SalaryMonthDTO currentMonth = null;

            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                SalaryMonthDTO newMonth = tryParseMonthHeader(row, dateColIndex);
                if (newMonth != null) {
                    currentMonth = newMonth;
                    result.add(currentMonth);
                    log.trace("SalaryService: Found new month block: {}", currentMonth.getMonthName());
                    continue;
                }

                if (currentMonth == null) {
                    continue;
                }

                Optional<SalaryRowDTO> rowDTO = parseDataRow(row, mappings, colorMap, currentMonth);
                if (rowDTO.isPresent()) {
                    currentMonth.getRows().add(rowDTO.get());
                }
            }
        } catch (IOException e) {
            log.error("SalaryService: Error parsing salary file for user " + username, e);
        }

        return result;
    }

    private SalaryMonthDTO tryParseMonthHeader(Row row, int dateColIndex) {
        Cell dateCell = row.getCell(dateColIndex);
        String dateVal = ExcelHelperService.getCellStringValue(dateCell);

        if (dateVal.isEmpty()) {
            return null;
        }

        Matcher matcher = MONTH_HEADER_PATTERN.matcher(dateVal);
        if (matcher.find()) {
            String monthName = matcher.group(1).toUpperCase();
            int year = Integer.parseInt(matcher.group(2));

            return new SalaryMonthDTO(monthName, year, BigDecimal.ZERO, new ArrayList<>());
        }
        return null;
    }

    private Optional<SalaryRowDTO> parseDataRow(Row row, List<SalaryColumnMapping> mappings, Map<String, String> colorMap, SalaryMonthDTO currentMonth) {
        SalaryRowDTO rowDTO = new SalaryRowDTO(new HashMap<>(), new HashMap<>());
        boolean hasData = false;

        for (SalaryColumnMapping mapping : mappings) {
            Cell cell = row.getCell(mapping.getExcelColIndex());
            String cellValue = ExcelHelperService.getCellStringValue(cell);

            if (!cellValue.isEmpty()) {
                if (mapping.isCurrency()) {
                    if (!isZeroCurrency(cellValue)) {
                        hasData = true;
                    }
                } else {
                    hasData = true;
                }
            }

            if (mapping.isSalary()) {
                updateTotalAmount(currentMonth, cellValue);
            }

            if (mapping.isVisible()) {
                if (mapping.isCurrency()) {
                    cellValue = DataCleaningService.formatCurrency(cellValue);
                }
                rowDTO.getColumnValues().put(mapping.getExcelColIndex(), cellValue);

                if (mapping.isUseExcelColor()) {
                    String excelColor = ExcelHelperService.getCellColorHex(cell);
                    if (excelColor != null && colorMap.containsKey(excelColor)) {
                        rowDTO.getColumnColors().put(mapping.getExcelColIndex(), colorMap.get(excelColor));
                    }
                }
            }
        }

        return hasData ? Optional.of(rowDTO) : Optional.empty();
    }

    private boolean isZeroCurrency(String value) {
        if (value == null || value.isEmpty()) return true;
        String clean = value.replaceAll("[^\\d.,]", "").replace(",", ".");
        if (clean.isEmpty()) return true;
        try {
            return new BigDecimal(clean).compareTo(BigDecimal.ZERO) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void updateTotalAmount(SalaryMonthDTO currentMonth, String cellValue) {
        try {
            String cleanValue = cellValue.replaceAll("[^\\d.,]", "").replace(",", ".");
            if (!cleanValue.isEmpty()) {
                currentMonth.setTotalAmount(currentMonth.getTotalAmount().add(new BigDecimal(cleanValue)));
            }
        } catch (Exception e) {
            log.debug("SalaryService: Failed to parse salary value from cell: {}", cellValue);
        }
    }
}
