package og.portal.zarplata.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import og.portal.zarplata.dto.GeneratedFileDTO;
import og.portal.zarplata.model.InvoiceParseSetting;
import og.portal.zarplata.model.SupplierSetting;
import og.portal.zarplata.repository.InvoiceParseSettingRepository;
import og.portal.zarplata.repository.SupplierSettingRepository;
import og.portal.zarplata.service.excel.OrderGeneratorService;
import og.portal.zarplata.service.excel.ExcelHelperService;
import og.portal.zarplata.service.util.DataCleaningService;
import og.portal.zarplata.service.util.WindowsExplorerComparator;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

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

        Map<SupplierSetting, Map<String, Integer>> aggregatedData = new LinkedHashMap<>();

        List<MultipartFile> sortedFiles = sortFilesByName(files);

        for (MultipartFile file : sortedFiles) {
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

                    if (shouldSkipRow(row, parseSetting)) {
                        log.info("Row {}: Item number column has color, skipping.", i + 1);
                        continue;
                    }

                    String article = getArticle(row, articleCell, parseSetting);
                    int quantity = getQuantity(row, parseSetting);

                    Optional<SupplierSetting> currentSupplier = getCurrentSupplier(articleCell, defaultSupplier, i, supplierByColor);

                    if (currentSupplier.isPresent()) {
                        aggregatedData.computeIfAbsent(currentSupplier.get(), k -> new LinkedHashMap<>())
                                .merge(article, quantity, Integer::sum);
                    }
                }
            }
        }

        return createGeneratedFiles(aggregatedData);
    }

    private List<MultipartFile> sortFilesByName(MultipartFile[] files) {
        List<MultipartFile> fileList = new ArrayList<>(List.of(files));
        WindowsExplorerComparator comparator = new WindowsExplorerComparator();
        
        fileList.sort((f1, f2) -> {
            String name1 = f1.getOriginalFilename();
            String name2 = f2.getOriginalFilename();
            if (name1 == null) name1 = "";
            if (name2 == null) name2 = "";
            return comparator.compare(name1, name2);
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

    private boolean shouldSkipRow(Row row, InvoiceParseSetting parseSetting) {
        Cell itemNumberCell = row.getCell(parseSetting.getItemNumberCol());
        if (itemNumberCell == null) {
            return false;
        }

        String hexColor = ExcelHelperService.getCellColorHex(itemNumberCell);
        
        return hexColor != null && !hexColor.equalsIgnoreCase(WHITE_COLOR);
    }

    private int getQuantity(Row row, InvoiceParseSetting parseSetting) {
        Cell quantityCell = row.getCell(parseSetting.getQuantityCol());
        if (quantityCell == null) return 0;

        if (quantityCell.getCellType() == CellType.NUMERIC) {
            return (int) quantityCell.getNumericCellValue();
        } else if (quantityCell.getCellType() == CellType.STRING) {
            String val = DataCleaningService.getLastDigits(quantityCell.getStringCellValue());
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
                     String val = DataCleaningService.getLastDigits(quantityCell.getStringCellValue());
                     if (!val.isEmpty()) return Integer.parseInt(val);
                 } catch (Exception ex) {
                     log.warn("Failed to evaluate formula for quantity: {}", ex.getMessage());
                 }
             }
        }

        return 0;
    }

    private String getArticle(Row row, Cell articleCell, InvoiceParseSetting parseSetting) {
        int supplierArticleColIndex = parseSetting.getSupplierArticleCol();
        Cell supplierArticleCell = row.getCell(supplierArticleColIndex);
        if (supplierArticleCell != null && supplierArticleCell.getCellType() != CellType.BLANK && supplierArticleCell.getCellType() != CellType.ERROR) {
            String supplierArticle = DataCleaningService.getDigits(ExcelHelperService.getCellStringValue(supplierArticleCell));
            if (!supplierArticle.isEmpty()) {
                return supplierArticle;
            }
        }

        return DataCleaningService.trimNonDigits(ExcelHelperService.getCellStringValue(articleCell));
    }

    private Optional<SupplierSetting> getCurrentSupplier(Cell articleCell, SupplierSetting defaultSupplier, int i, Map<String, SupplierSetting> supplierByColor) {
        String argbHex = ExcelHelperService.getCellColorHex(articleCell);
        
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

    private List<GeneratedFileDTO> createGeneratedFiles(Map<SupplierSetting, Map<String, Integer>> aggregatedData) throws IOException {
        List<GeneratedFileDTO> result = new ArrayList<>();
        for (Map.Entry<SupplierSetting, Map<String, Integer>> entry : aggregatedData.entrySet()) {
            SupplierSetting supplier = entry.getKey();
            Map<String, Integer> articles = entry.getValue();

            byte[] excelData = OrderGeneratorService.createOrderFile(articles);
            result.add(new GeneratedFileDTO(supplier.getFileName(), excelData));
            
            log.info("Generated file: {}", supplier.getFileName());
        }
        return result;
    }
}
