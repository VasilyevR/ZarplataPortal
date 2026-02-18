package og.portal.zarplata.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import og.portal.zarplata.model.Invoice;
import og.portal.zarplata.repository.InvoiceRepository;
import og.portal.zarplata.service.excel.SalaryExcelParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalarySyncService {

    @Value("${app.share.folder.path}")
    private String salaryFolderPath;

    private final InvoiceRepository invoiceRepository;
    private final SalaryExcelParser salaryExcelParser;

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
                processFile(file);
            } catch (Exception e) {
                log.error("Error processing file: " + file.getName(), e);
            }
        }
        log.info("Salary synchronization finished.");
    }

    private void processFile(File file) throws Exception {
        Map<String, BigDecimal> salaryData = salaryExcelParser.parseSalaryFile(file);
        
        for (Map.Entry<String, BigDecimal> entry : salaryData.entrySet()) {
            String invoiceNumber = entry.getKey();
            BigDecimal amount = entry.getValue();
            
            updateInvoice(invoiceNumber, amount);
        }
    }

    private void updateInvoice(String invoiceNumber, BigDecimal amount) {
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
