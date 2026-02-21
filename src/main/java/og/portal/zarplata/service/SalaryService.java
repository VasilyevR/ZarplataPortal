package og.portal.zarplata.service;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import og.portal.zarplata.dto.SalaryMonthDTO;
import og.portal.zarplata.dto.SalaryRowDTO;
import og.portal.zarplata.model.ColorMapping;
import og.portal.zarplata.model.SalaryColumnMapping;
import og.portal.zarplata.repository.ColorMappingRepository;
import og.portal.zarplata.repository.SalaryColumnMappingRepository;
import og.portal.zarplata.service.excel.ExcelHelperService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalaryService {

    @Value("${app.share.folder.path}")
    private String shareFolderPath;

    @Value("${app.salary.folder.path}")
    private String salaryFolderPath;

    private final SalaryColumnMappingRepository columnMappingRepository;
    private final ColorMappingRepository colorMappingRepository;

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
        File file = new File(shareFolderPath + salaryFolderPath, username + ".xlsx");
        if (!file.exists()) {
            log.warn("SalaryService: Salary file not found for user: {} (Path: {})", username, file.getAbsolutePath());
            return Collections.emptyList();
        }

        log.debug("SalaryService: Reading file: {}", file.getAbsolutePath());

        List<SalaryColumnMapping> mappings = columnMappingRepository.findAll();
        Map<String, String> colorMap = colorMappingRepository.findAll().stream()
                .collect(Collectors.toMap(ColorMapping::getExcelArgbHex, ColorMapping::getHtmlColorCode, (a, b) -> a));

        List<SalaryMonthDTO> result = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(file); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            SalaryMonthDTO currentMonth = null;

            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell firstCell = row.getCell(0);
                String firstVal = ExcelHelperService.getCellStringValue(firstCell);

                if (firstVal.isEmpty()) {
                    currentMonth = null;
                    continue;
                }

                if (currentMonth == null) {
                    Integer year = null;
                    java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\b(20\\d{2})\\b").matcher(firstVal);
                    if (matcher.find()) {
                        year = Integer.parseInt(matcher.group(1));
                    }
                    
                    currentMonth = new SalaryMonthDTO(firstVal, year, BigDecimal.ZERO, new ArrayList<>());
                    result.add(currentMonth);
                    log.trace("SalaryService: Found new month block: {}", firstVal);
                    
                    if (ExcelHelperService.getCellStringValue(row.getCell(1)).isEmpty()) continue;
                }

                SalaryRowDTO rowDTO = new SalaryRowDTO(new HashMap<>(), new HashMap<>());
                for (SalaryColumnMapping mapping : mappings) {
                    Cell cell = row.getCell(mapping.getExcelColIndex());
                    String cellValue = ExcelHelperService.getCellStringValue(cell);

                    if (mapping.isSalary()) {
                        try {
                            String cleanValue = cellValue.replaceAll("[^\\d.,]", "").replace(",", ".");
                            if (!cleanValue.isEmpty()) {
                                currentMonth.setTotalAmount(currentMonth.getTotalAmount().add(new java.math.BigDecimal(cleanValue)));
                            }
                        } catch (Exception e) {
                            log.debug("SalaryService: Failed to parse salary value from cell: {}", cellValue);
                        }
                    }

                    if (mapping.isVisible()) {
                        rowDTO.getColumnValues().put(mapping.getExcelColIndex(), cellValue);
                        if (mapping.isUseExcelColor()) {
                            String excelColor = ExcelHelperService.getCellColorHex(cell);
                            if (excelColor != null && colorMap.containsKey(excelColor)) {
                                rowDTO.getColumnColors().put(mapping.getExcelColIndex(), colorMap.get(excelColor));
                            }
                        }
                    }
                }
                currentMonth.getRows().add(rowDTO);
            }
        } catch (IOException e) {
            log.error("SalaryService: Error parsing salary file for user " + username, e);
        }

        return result;
    }
}
