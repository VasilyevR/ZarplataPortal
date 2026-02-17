package og.portal.zarplata.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import og.portal.zarplata.dto.GeneratedFileDTO;
import og.portal.zarplata.model.InvoiceParseSetting;
import og.portal.zarplata.model.SupplierSetting;
import og.portal.zarplata.repository.InvoiceParseSettingRepository;
import og.portal.zarplata.repository.SupplierSettingRepository;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private static final String WHITE_COLOR = "FFFFFFFF";

    private final SupplierSettingRepository supplierSettingRepository;
    private final InvoiceParseSettingRepository invoiceParseSettingRepository;

    public List<GeneratedFileDTO> generatePurchaseOrders(MultipartFile[] files) throws IOException {
        InvoiceParseSetting parseSetting = invoiceParseSettingRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Invoice parsing settings not found in DB"));

        Map<String, SupplierSetting> supplierByColor = getSuppliers();
        SupplierSetting defaultSupplier = supplierSettingRepository.findByIsDefaultTrue()
                .orElseThrow(() -> new IllegalStateException("Default supplier (isDefault=true) not found"));

        Map<SupplierSetting, Map<String, Integer>> aggregatedData = new HashMap<>();

        List<MultipartFile> sortedFiles = sortFilesByName(files);

        for (MultipartFile file : sortedFiles) {
            log.info("Processing file: {}", file.getOriginalFilename());
            try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
                Sheet sheet = workbook.getSheetAt(0);
                for (int i = parseSetting.getStartRow(); i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    Cell articleCell = row.getCell(parseSetting.getArticleCol());

                    if (isInvoiceEnded(articleCell)) {
                        log.info("Found empty article cell at row {}, stopping processing for file {}", i + 1, file.getOriginalFilename());
                        break;
                    }

                    if (shouldSkipRow(row, parseSetting, workbook)) {
                        log.info("Row {}: Item number column has color, skipping.", i + 1);
                        continue;
                    }

                    String article = getArticle(row, articleCell, parseSetting);
                    int quantity = getQuantity(row, parseSetting);

                    Optional<SupplierSetting> currentSupplier = getCurrentSupplier(articleCell, defaultSupplier, i, supplierByColor, workbook);

                    if (currentSupplier.isPresent()) {
                        aggregatedData.computeIfAbsent(currentSupplier.get(), k -> new HashMap<>())
                                .merge(article, quantity, Integer::sum);
                    }
                }
            }
        }

        return createGeneratedFiles(aggregatedData);
    }

    private List<MultipartFile> sortFilesByName(MultipartFile[] files) {
        List<MultipartFile> fileList = new ArrayList<>(List.of(files));
        Collator collator = Collator.getInstance(new Locale("ru", "RU"));
        
        fileList.sort((f1, f2) -> {
            String name1 = f1.getOriginalFilename();
            String name2 = f2.getOriginalFilename();
            if (name1 == null) name1 = "";
            if (name2 == null) name2 = "";
            return collator.compare(name1, name2);
        });
        return fileList;
    }

    private Map<String, SupplierSetting> getSuppliers() {
        List<SupplierSetting> allSuppliers = supplierSettingRepository.findAll();
        Map<String, SupplierSetting> supplierByColor = new HashMap<>();
        for (SupplierSetting s : allSuppliers) {
            if (s.getColorHex() != null) {
                supplierByColor.put(s.getColorHex(), s);
            }
        }
        return supplierByColor;
    }

    private static boolean isInvoiceEnded(Cell articleCell) {
        return articleCell == null || articleCell.getCellType() == CellType.BLANK || articleCell.getCellType() == CellType.ERROR;
    }

    private static boolean shouldSkipRow(Row row, InvoiceParseSetting parseSetting, Workbook workbook) {
        Cell itemNumberCell = row.getCell(parseSetting.getItemNumberCol());
        if (itemNumberCell == null) {
            return false;
        }

        String hexColor = getCellColorHex(itemNumberCell, workbook);
        
        return hexColor != null && !hexColor.equalsIgnoreCase(WHITE_COLOR);
    }

    private static int getQuantity(Row row, InvoiceParseSetting parseSetting) {
        Cell quantityCell = row.getCell(parseSetting.getQuantityCol());
        if (quantityCell == null) return 0;

        if (quantityCell.getCellType() == CellType.NUMERIC) {
            return (int) quantityCell.getNumericCellValue();
        } else if (quantityCell.getCellType() == CellType.STRING) {
            String val = getLastDigits(quantityCell.getStringCellValue());
            if (!val.isEmpty()) {
                try {
                    return Integer.parseInt(val);
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse quantity from string: {}", quantityCell.getStringCellValue());
                }
            }
        } else if (quantityCell.getCellType() == CellType.FORMULA) {
             try {
                 return (int) quantityCell.getNumericCellValue();
             } catch (IllegalStateException e) {
                 try {
                     String val = getLastDigits(quantityCell.getStringCellValue());
                     if (!val.isEmpty()) return Integer.parseInt(val);
                 } catch (Exception ex) {
                     log.warn("Failed to evaluate formula for quantity: {}", ex.getMessage());
                 }
             }
        }

        return 0;
    }

    private static String getArticle(Row row, Cell articleCell, InvoiceParseSetting parseSetting) {
        int supplierArticleColIndex = parseSetting.getSupplierArticleCol();
        Cell supplierArticleCell = row.getCell(supplierArticleColIndex);
        if (supplierArticleCell != null && supplierArticleCell.getCellType() != CellType.BLANK && supplierArticleCell.getCellType() != CellType.ERROR) {
            String supplierArticle = getDigits(getCellStringValue(supplierArticleCell));
            if (!supplierArticle.isEmpty()) {
                return supplierArticle;
            }
        }

        return trimNonDigits(getCellStringValue(articleCell));
    }

    private static Optional<SupplierSetting> getCurrentSupplier(Cell articleCell, SupplierSetting defaultSupplier, int i, Map<String, SupplierSetting> supplierByColor, Workbook workbook) {
        String argbHex = getCellColorHex(articleCell, workbook);
        
        if (argbHex == null) {
            log.info("Row {}: No fill color found. Using default supplier.", i + 1);
            return Optional.of(defaultSupplier);
        }

        log.info("Row {}: Found color ARGB HEX: {}", i + 1, argbHex);
        
        if (supplierByColor.containsKey(argbHex)) {
            return Optional.of(supplierByColor.get(argbHex));
        } else {
            log.warn("Row {}: Color {} found but no supplier mapped. Skipping row.", i + 1, argbHex);
            return Optional.empty();
        }
    }

    private static String getCellColorHex(Cell cell, Workbook workbook) {
        CellStyle style = cell.getCellStyle();
        Color color = style.getFillForegroundColorColor();

        if (color == null) {
            return null;
        }

        if (color instanceof XSSFColor) {
            XSSFColor xssfColor = (XSSFColor) color;
            if (xssfColor.isAuto()) return null;
            return xssfColor.getARGBHex();
        } else if (color instanceof HSSFColor) {
            HSSFColor hssfColor = (HSSFColor) color;
            short[] triplet = hssfColor.getTriplet();
            if (triplet == null) return null;
            return String.format("FF%02X%02X%02X", triplet[0], triplet[1], triplet[2]).toUpperCase();
        } else if (workbook instanceof HSSFWorkbook) {
             short index = style.getFillForegroundColor();
             HSSFColor hssfColor = ((HSSFWorkbook) workbook).getCustomPalette().getColor(index);
             if (hssfColor != null) {
                 short[] triplet = hssfColor.getTriplet();
                 return String.format("FF%02X%02X%02X", triplet[0], triplet[1], triplet[2]).toUpperCase();
             }
        }
        
        return null;
    }

    private static String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long)cell.getNumericCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (IllegalStateException e) {
                    return String.valueOf((long)cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case ERROR:
                return "";
            default:
                return "";
        }
    }

    private static String getLastDigits(String text) {
        if (text == null) {
            return "";
        }
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(text);

        String lastMatch = "";
        while (matcher.find()) {
            lastMatch = matcher.group();
        }

        return lastMatch;
    }

    private static String getDigits(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\D", "");
    }

    private static String trimNonDigits(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("^\\D+|\\D+$", "");
    }

    private static byte[] createExcelFile(Map<String, Integer> articles) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream fileOut = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Order");
            int rowNum = 0;

            for (Map.Entry<String, Integer> articleEntry : articles.entrySet()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(articleEntry.getKey());
                row.createCell(1).setCellValue(articleEntry.getValue());
            }

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            workbook.write(fileOut);
            return fileOut.toByteArray();
        }
    }

    private List<GeneratedFileDTO> createGeneratedFiles(Map<SupplierSetting, Map<String, Integer>> aggregatedData) throws IOException {
        List<GeneratedFileDTO> result = new ArrayList<>();
        for (Map.Entry<SupplierSetting, Map<String, Integer>> entry : aggregatedData.entrySet()) {
            SupplierSetting supplier = entry.getKey();
            Map<String, Integer> articles = entry.getValue();

            byte[] excelData = createExcelFile(articles);
            result.add(new GeneratedFileDTO(supplier.getFileName(), excelData));
            
            log.info("Generated file: {}", supplier.getFileName());
        }
        return result;
    }
}
