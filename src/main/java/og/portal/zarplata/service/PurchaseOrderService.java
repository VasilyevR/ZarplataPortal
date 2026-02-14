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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final SupplierSettingRepository supplierSettingRepository;
    private final InvoiceParseSettingRepository invoiceParseSettingRepository;

    public byte[] generatePurchaseOrders(MultipartFile[] files) throws IOException {
        List<SupplierSetting> allSuppliers = supplierSettingRepository.findAll();
        InvoiceParseSetting parseSetting = invoiceParseSettingRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Invoice parsing settings not found in DB"));

        Map<String, SupplierSetting> supplierByColor = new HashMap<>();
        for (SupplierSetting s : allSuppliers) {
            if (s.getColorHex() != null) {
                supplierByColor.put(s.getColorHex(), s);
            }
        }
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
                    Cell quantityCell = row.getCell(parseSetting.getQuantityCol());
                    Optional<Cell> supplierArticleCell = Optional.ofNullable(row.getCell(parseSetting.getSupplierArticleCol()));

                    if (articleCell == null || articleCell.getCellType() == CellType.BLANK) {
                        log.info("Found empty article cell at row {}, stopping processing for file {}", i + 1, file.getOriginalFilename());
                        break;
                    }

                    String article = supplierArticleCell.isPresent()
                            ? getCellStringValue(supplierArticleCell.get())
                            : getCellStringValue(articleCell);
                    int quantity = (int) quantityCell.getNumericCellValue();

                    CellStyle style = articleCell.getCellStyle();
                    Color color = style.getFillForegroundColorColor();
                    SupplierSetting currentSupplier = defaultSupplier;

                    if (color instanceof XSSFColor) {
                        String argbHex = ((XSSFColor) color).getARGBHex();
                        log.info("Row {}: Found color ARGB HEX: {}", i + 1, argbHex);
                        
                        if (argbHex != null && supplierByColor.containsKey(argbHex)) {
                            currentSupplier = supplierByColor.get(argbHex);
                        }
                    } else {
                         log.info("Row {}: No XSSFColor found (likely default/no fill).", i + 1);
                    }

                    aggregatedData.computeIfAbsent(currentSupplier, k -> new HashMap<>())
                            .merge(article, quantity, Integer::sum);
                }
            }
        }

        return createZipArchive(aggregatedData);
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
}
