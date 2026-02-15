package og.portal.zarplata.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import og.portal.zarplata.model.InvoiceParseSetting;
import og.portal.zarplata.model.SupplierSetting;
import og.portal.zarplata.repository.InvoiceParseSettingRepository;
import og.portal.zarplata.repository.SupplierSettingRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final SupplierSettingRepository supplierSettingRepository;
    private final InvoiceParseSettingRepository invoiceParseSettingRepository;

    public byte[] generatePurchaseOrders(MultipartFile[] files) throws IOException {
        InvoiceParseSetting parseSetting = invoiceParseSettingRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Invoice parsing settings not found in DB"));

        Map<String, SupplierSetting> supplierByColor = getSuppliers();
        SupplierSetting defaultSupplier = supplierSettingRepository.findByColorHexIsNull()
                .orElseThrow(() -> new IllegalStateException("Default supplier (no color) not found"));

        Map<SupplierSetting, Map<String, Integer>> aggregatedData = new HashMap<>();

        for (MultipartFile file : files) {
            log.info("Processing file: {}", file.getOriginalFilename());
            try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
                Sheet sheet = workbook.getSheetAt(0);
                for (int i = parseSetting.getStartRow(); i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    Cell articleCell = row.getCell(parseSetting.getArticleCol());

                    if (isInvoiceEnded(articleCell)) {
                        log.info("Found empty article cell at row {}, stopping processing for file {}", i + 1, file.getOriginalFilename());
                        break;
                    }

                    String article = getArticle(row, articleCell, parseSetting);

                    int quantity = getQuantity(row, parseSetting);

                    Optional<SupplierSetting> currentSupplier = getCurrentSupplier(articleCell, defaultSupplier, i, supplierByColor);

                    if (currentSupplier.isPresent()) {
                        aggregatedData.computeIfAbsent(currentSupplier.get(), k -> new HashMap<>())
                                .merge(article, quantity, Integer::sum);
                    }
                }
            }
        }

        return createZipArchive(aggregatedData);
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
        return articleCell == null || articleCell.getCellType() == CellType.BLANK;
    }

    private int getQuantity(Row row, InvoiceParseSetting parseSetting) {
        Cell quantityCell = row.getCell(parseSetting.getQuantityCol());

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
        }

        return 0;
    }

    private String getArticle(Row row, Cell articleCell, InvoiceParseSetting parseSetting) {
        Cell supplierArticleCell = row.getCell(parseSetting.getSupplierArticleCol());
        if (supplierArticleCell != null && supplierArticleCell.getCellType() != CellType.BLANK) {
            String supplierArticle = getDigits(getCellStringValue(supplierArticleCell));
            if (!supplierArticle.isEmpty()) {
                return supplierArticle;
            }
        }

        return getCellStringValue(articleCell);
    }

    private static Optional<SupplierSetting> getCurrentSupplier(Cell articleCell, SupplierSetting defaultSupplier, int i, Map<String, SupplierSetting> supplierByColor) {
        CellStyle style = articleCell.getCellStyle();
        Color color = style.getFillForegroundColorColor();
        
        if (!(color instanceof XSSFColor)) {
            log.info("Row {}: No fill color found. Using default supplier.", i + 1);
            return Optional.of(defaultSupplier);
        }

        XSSFColor xssfColor = (XSSFColor) color;
        if (xssfColor.isAuto() || xssfColor.getARGBHex() == null) {
             log.info("Row {}: Color is AUTO or NULL. Using default supplier.", i + 1);
             return Optional.of(defaultSupplier);
        }

        String argbHex = xssfColor.getARGBHex();
        log.info("Row {}: Found color ARGB HEX: {}", i + 1, argbHex);
        
        if (supplierByColor.containsKey(argbHex)) {
            return Optional.of(supplierByColor.get(argbHex));
        } else {
            log.warn("Row {}: Color {} found but no supplier mapped. Skipping row.", i + 1, argbHex);
            return Optional.empty();
        }
    }

    private byte[] createZipArchive(Map<SupplierSetting, Map<String, Integer>> aggregatedData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Map.Entry<SupplierSetting, Map<String, Integer>> entry : aggregatedData.entrySet()) {
                SupplierSetting supplier = entry.getKey();
                Map<String, Integer> articles = entry.getValue();

                byte[] excelData = createExcelFile(articles);

                ZipEntry zipEntry = new ZipEntry(supplier.getFileName());
                zos.putNextEntry(zipEntry);
                zos.write(excelData);
                zos.closeEntry();
                log.info("Added {} to ZIP archive.", supplier.getFileName());
            }
        }
        return baos.toByteArray();
    }

    private byte[] createExcelFile(Map<String, Integer> articles) throws IOException {
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

    private String getCellStringValue(Cell cell) {
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long)cell.getNumericCellValue());
        }
        return cell.getStringCellValue();
    }

    private String getLastDigits(String text) {
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

    private String getDigits(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\D", "");
    }
}
