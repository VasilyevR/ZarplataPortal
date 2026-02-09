package og.portal.zarplata.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import og.portal.zarplata.model.Invoice;
import og.portal.zarplata.repository.InvoiceRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelSyncService {

    @Value("${app.share.folder.path}")
    private String salaryFolderPath;

    private final InvoiceRepository invoiceRepository;

//    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void syncSalaries() {
        log.info("Starting salary synchronization from {}", salaryFolderPath);
        
        File folder = new File(salaryFolderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            log.error("Salary folder not found or is not a directory: {}", salaryFolderPath);
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".xlsx"));
        if (files == null) return;

        for (File file : files) {
            try {
                processExcelFile(file);
            } catch (Exception e) {
                log.error("Error processing file: " + file.getName(), e);
            }
        }
        log.info("Salary synchronization finished.");
    }

    private void processExcelFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                String invoiceNumber = getCellValueAsString(row.getCell(0));
                if (invoiceNumber == null || invoiceNumber.trim().isEmpty()) {
                    continue;
                }

                BigDecimal amount = getCellValueAsBigDecimal(row.getCell(1));
                
                Optional<Invoice> invoiceOpt = invoiceRepository.findByNumber(invoiceNumber);
                if (invoiceOpt.isPresent()) {
                    Invoice invoice = invoiceOpt.get();
                    invoice.setGivenSum(amount);
                    invoiceRepository.save(invoice);
                    log.debug("Updated invoice {} with amount {}", invoiceNumber, amount);
                } else {
                    log.warn("Invoice {} not found in DB, skipping update.", invoiceNumber);
                }
            }
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double val = cell.getNumericCellValue();
                if (val == (long) val) {
                    return String.format("%d", (long) val);
                }
                return String.valueOf(val);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (IllegalStateException e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }

    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) return BigDecimal.ZERO;
        switch (cell.getCellType()) {
            case NUMERIC:
                return BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING:
                try {
                    String val = cell.getStringCellValue().replace(",", ".").trim();
                    if (val.isEmpty()) return BigDecimal.ZERO;
                    return new BigDecimal(val);
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse BigDecimal from string: {}", cell.getStringCellValue());
                    return BigDecimal.ZERO;
                }
            case FORMULA:
                try {
                    return BigDecimal.valueOf(cell.getNumericCellValue());
                } catch (IllegalStateException e) {
                    return BigDecimal.ZERO;
                }
            default:
                return BigDecimal.ZERO;
        }
    }
}
