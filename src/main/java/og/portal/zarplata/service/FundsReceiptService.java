package og.portal.zarplata.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import og.portal.zarplata.dto.BankStatementSaveRequestDTO;
import og.portal.zarplata.dto.BankStatementSearchResultDTO;
import og.portal.zarplata.dto.UserDTO;
import og.portal.zarplata.model.BankStatementSetting;
import og.portal.zarplata.model.SalaryColumnMapping;
import og.portal.zarplata.model.SalaryParseSetting;
import og.portal.zarplata.repository.BankStatementSettingRepository;
import og.portal.zarplata.repository.SalaryColumnMappingRepository;
import og.portal.zarplata.repository.SalaryParseSettingRepository;
import og.portal.zarplata.service.excel.ExcelHelperService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FundsReceiptService {

    private static final String PAID_AMOUNT_COLUMN_NAME = "Сумма заказа";
    private static final String PAYMENT_DATE_COLUMN_NAME = "Дата прихода";
    private static final String CLIENT_NAME_COLUMN_NAME = "Название фирмы";
    private static final String ORDER_DATE_COLUMN_NAME = "Дата";
    private static final String EXCEL_EXTENSION = ".xlsx";

    @Value("${app.share.folder.path}")
    private String shareFolderPath;

    @Value("${app.salary.folder.path}")
    private String salaryFolderPath;
    
    @Value("${app.date.format}")
    private String appDateFormat;

    private final BankStatementSettingRepository bankStatementSettingRepository;
    private final SalaryParseSettingRepository salaryParseSettingRepository;
    private final SalaryColumnMappingRepository salaryColumnMappingRepository;
    private final UserService userService;

    public List<BankStatementSearchResultDTO> search(MultipartFile file, String bankName) throws IOException {
        log.info("Starting search for bank: {}", bankName);
        BankStatementSetting bankSetting = bankStatementSettingRepository.findByBankName(bankName)
                .orElseThrow(() -> {
                    log.error("Bank settings not found for: {}", bankName);
                    return new IllegalArgumentException("Bank settings not found for: " + bankName);
                });

        List<BankStatementSearchResultDTO> results = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(appDateFormat);

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int startRow = bankSetting.getStartRow();
            int lastRow = sheet.getLastRowNum();
            log.debug("Processing bank statement rows from {} to {}", startRow, lastRow);

            for (int i = startRow; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                BigDecimal amount = ExcelHelperService.getCellBigDecimalValue(row.getCell(bankSetting.getAmountColIndex()));
                if (amount == null) continue;

                String dateString = ExcelHelperService.getCellStringValue(row.getCell(bankSetting.getDateColIndex()));
                if (dateString == null) continue;

                LocalDate date = LocalDate.parse(dateString,  formatter);

                String clientName = ExcelHelperService.getCellStringValue(row.getCell(bankSetting.getClientNameColIndex()));

                List<BankStatementSearchResultDTO> matches = findMatchesInSalaryFiles(amount, date, clientName, formatter);
                if (matches.isEmpty()) {
                    BankStatementSearchResultDTO notFound = new BankStatementSearchResultDTO();
                    notFound.setAmount(amount);
                    notFound.setDate(date.format(formatter));
                    notFound.setClientName(clientName);
                    notFound.setFound(false);
                    results.add(notFound);
                } else {
                    results.addAll(matches);
                }
            }
        }
        log.info("Search completed. Found {} results.", results.size());
        return results;
    }

    private List<BankStatementSearchResultDTO> findMatchesInSalaryFiles(BigDecimal amount, LocalDate date, String clientName, DateTimeFormatter formatter) {
        log.debug("Searching for matches: amount={}, date={}, client={}", amount, date, clientName);
        List<BankStatementSearchResultDTO> matches = new ArrayList<>();
        List<UserDTO> users = userService.getAllUsersSortedByLogin();

        if (users.isEmpty()) {
            log.warn("No users found.");
            return matches;
        }

        SalaryParseSetting parseSetting = salaryParseSettingRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Salary parsing settings not found"));

        int paidAmountColIndex = -1; 
        int paymentDateColIndex = -1;
        int clientNameColIndex = -1;
        int orderDateColIndex = -1;

        List<SalaryColumnMapping> mappings = salaryColumnMappingRepository.findAll();
        for(SalaryColumnMapping mapping : mappings) {
             if(PAID_AMOUNT_COLUMN_NAME.equalsIgnoreCase(mapping.getColumnName())) {
                 paidAmountColIndex = mapping.getExcelColIndex();
             }
             if(PAYMENT_DATE_COLUMN_NAME.equalsIgnoreCase(mapping.getColumnName())) {
                 paymentDateColIndex = mapping.getExcelColIndex();
             }
             if(CLIENT_NAME_COLUMN_NAME.equalsIgnoreCase(mapping.getColumnName())) {
                 clientNameColIndex = mapping.getExcelColIndex();
             }
             if(ORDER_DATE_COLUMN_NAME.equalsIgnoreCase(mapping.getColumnName())) {
                 orderDateColIndex = mapping.getExcelColIndex();
             }
        }
        
        if(paidAmountColIndex == -1 || paymentDateColIndex == -1 || clientNameColIndex == -1 || orderDateColIndex == -1) {
            log.warn("Column mappings not found: Paid Amount={}, Payment Date={}, Client Name={}, Order date={}",
                    paidAmountColIndex, paymentDateColIndex, clientNameColIndex, orderDateColIndex);
            return matches;
        }

        for (UserDTO user : users) {
            File file = new File(shareFolderPath + salaryFolderPath, user.getLogin() + EXCEL_EXTENSION);
            if (!file.exists()) {
                log.trace("Skipping user {}: file {} not found", user.getLogin(), file.getPath());
                continue;
            }

            try (Workbook workbook = new XSSFWorkbook(new FileInputStream(file))) {
                Sheet sheet = workbook.getSheetAt(0);
                for (int i = parseSetting.getStartRow(); i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    BigDecimal paidAmount = ExcelHelperService.getCellBigDecimalValue(row.getCell(paidAmountColIndex));
                    if (paidAmount != null && paidAmount.compareTo(amount) == 0) {
                        Cell paymentDateCell = row.getCell(paymentDateColIndex);
                        
                        boolean isCellEmpty = paymentDateCell == null ||
                                              paymentDateCell.getCellType() == CellType.BLANK ||
                                              (paymentDateCell.getCellType() == CellType.STRING && paymentDateCell.getStringCellValue().trim().isEmpty());
                                              
                        if (isCellEmpty) {
                            LocalDate orderDate = ExcelHelperService.getCellDateValue(row.getCell(orderDateColIndex));
                            if (orderDate != null && orderDate.isBefore(LocalDate.now().minusMonths(3))) {
                                log.debug("Found row match for amount but skipped due to old order date: {} in file {}", orderDate, file.getName());
                                continue;
                            }

                            BankStatementSearchResultDTO match = new BankStatementSearchResultDTO();
                            match.setAmount(amount);
                            match.setDate(date.format(formatter));
                            match.setClientName(clientName);
                            match.setManagerLogin(user.getLogin());
                            match.setFileName(file.getName());
                            match.setRowNumber(i);
                            match.setFound(true);
                            if (orderDate != null) {
                                match.setOrderDate(orderDate.format(formatter));
                            }
                            
                            String salaryClientName = ExcelHelperService.getCellStringValue(row.getCell(clientNameColIndex));
                            match.setPossibleClients(Collections.singletonList(salaryClientName));
                            
                            log.info("Match found! File: {}, Row: {}, Amount: {}", file.getName(), i, paidAmount);
                            matches.add(match);
                        } else {
                            log.debug("Found row match ({}) but skipped because Payment Date is already filled in file {} row {}", paidAmount, file.getName(), i);
                        }
                    }
                }
            } catch (IOException e) {
                log.error("Error reading file: " + file.getName(), e);
            }
        }
        return matches;
    }

    public void save(BankStatementSaveRequestDTO request) throws IOException {
        log.info("Saving payment date for file: {}, row: {}", request.getFileName(), request.getRowNumber());
        File file = new File(shareFolderPath + salaryFolderPath, request.getFileName());
        if (!file.exists()) {
            log.error("File not found: {}", file.getAbsolutePath());
            throw new IllegalArgumentException("File not found: " + request.getFileName());
        }

        int paymentDateColIndex = -1;
        List<SalaryColumnMapping> mappings = salaryColumnMappingRepository.findAll();
        for(SalaryColumnMapping mapping : mappings) {
             if(PAYMENT_DATE_COLUMN_NAME.equalsIgnoreCase(mapping.getColumnName())) {
                 paymentDateColIndex = mapping.getExcelColIndex();
                 break;
             }
        }
        
        if(paymentDateColIndex == -1) {
            log.error("Payment Date column mapping not found");
            throw new IllegalStateException("Payment Date column mapping not found");
        }

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row row = sheet.getRow(request.getRowNumber());
            if (row == null) {
                row = sheet.createRow(request.getRowNumber());
            }

            Cell cell = row.getCell(paymentDateColIndex);
            if (cell == null) {
                cell = row.createCell(paymentDateColIndex);
            }
            
            cell.setCellValue(request.getDate());

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
        log.info("Successfully saved payment date.");
    }
}
