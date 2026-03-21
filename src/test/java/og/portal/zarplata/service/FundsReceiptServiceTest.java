package og.portal.zarplata.service;

import og.portal.zarplata.dto.BankStatementSaveRequestDTO;
import og.portal.zarplata.dto.BankStatementSearchResultDTO;
import og.portal.zarplata.dto.UserDTO;
import og.portal.zarplata.model.BankStatementSetting;
import og.portal.zarplata.model.SalaryColumnMapping;
import og.portal.zarplata.model.SalaryParseSetting;
import og.portal.zarplata.repository.BankStatementSettingRepository;
import og.portal.zarplata.repository.SalaryColumnMappingRepository;
import og.portal.zarplata.repository.SalaryParseSettingRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FundsReceiptServiceTest {

    public static final String BANK_DATE_FORMAT = "dd.MM.yyyy";
    public static final String SALARY_DATE_FORMAT = "dd.MM.yy.";
    @Mock
    private BankStatementSettingRepository bankStatementSettingRepository;

    @Mock
    private SalaryParseSettingRepository salaryParseSettingRepository;

    @Mock
    private SalaryColumnMappingRepository salaryColumnMappingRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private FundsReceiptService fundsReceiptService;

    @TempDir
    Path tempDir;

    private File salaryDir;

    @BeforeEach
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void setUp() {
        salaryDir = tempDir.resolve("salary").toFile();
        if (!salaryDir.exists()) {
            salaryDir.mkdirs();
        }
        ReflectionTestUtils.setField(fundsReceiptService, "shareFolderPath", tempDir.toString());
        ReflectionTestUtils.setField(fundsReceiptService, "salaryFolderPath", "/salary");
        ReflectionTestUtils.setField(fundsReceiptService, "appDateFormat", SALARY_DATE_FORMAT);
    }

    @Test
    void search_ShouldReturnMatches_WhenFoundInSalaryFiles() throws IOException {
        // Given
        String bankName = "TestBank";
        BankStatementSetting bankSetting = new BankStatementSetting();
        bankSetting.setBankName(bankName);
        bankSetting.setStartRow(1);
        bankSetting.setAmountColIndex(0);
        bankSetting.setDateColIndex(1);
        bankSetting.setClientNameColIndex(2);
        bankSetting.setDateFormat(BANK_DATE_FORMAT);

        when(bankStatementSettingRepository.findByBankName(bankName)).thenReturn(Optional.of(bankSetting));

        // Create bank statement file
        File bankFile = File.createTempFile("bank", ".xlsx");
        try (Workbook bankWorkbook = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(bankFile)) {
            Sheet bankSheet = bankWorkbook.createSheet();
            Row bankRow = bankSheet.createRow(1);
            bankRow.createCell(0).setCellValue(100.0);
            Cell dateCell = bankRow.createCell(1);
            dateCell.setCellValue(LocalDate.now().format(DateTimeFormatter.ofPattern(bankSetting.getDateFormat())));
            bankRow.createCell(2).setCellValue("Client A");

            bankWorkbook.write(fos);
        }
        MockMultipartFile multipartFile = new MockMultipartFile("file", "bank.xlsx", 
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 
                java.nio.file.Files.readAllBytes(bankFile.toPath()));

        // Create salary file
        File salaryFile = new File(salaryDir, "Manager1.xlsx");
        try (Workbook salaryWorkbook = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(salaryFile)) {
            Sheet salarySheet = salaryWorkbook.createSheet();
            Row salaryRow = salarySheet.createRow(1);
            salaryRow.createCell(0).setCellValue(100.0); // Paid Amount
            salaryRow.createCell(1); // Payment Date (empty)
            salaryRow.createCell(2).setCellValue("Client A"); // Client Name
            Cell orderDateCell = salaryRow.createCell(3); // Order Date
            orderDateCell.setCellValue(LocalDate.now().format(DateTimeFormatter.ofPattern(SALARY_DATE_FORMAT)));

            salaryWorkbook.write(fos);
        }

        UserDTO user = UserDTO.builder().login("Manager1").build();
        when(userService.getAllUsersSortedByLogin()).thenReturn(Collections.singletonList(user));

        SalaryParseSetting parseSetting = new SalaryParseSetting();
        parseSetting.setStartRow(1);
        when(salaryParseSettingRepository.findAll()).thenReturn(Collections.singletonList(parseSetting));

        SalaryColumnMapping paidAmountMapping = new SalaryColumnMapping();
        paidAmountMapping.setColumnName("Сумма заказа");
        paidAmountMapping.setExcelColIndex(0);

        SalaryColumnMapping paymentDateMapping = new SalaryColumnMapping();
        paymentDateMapping.setColumnName("Дата прихода");
        paymentDateMapping.setExcelColIndex(1);

        SalaryColumnMapping clientNameMapping = new SalaryColumnMapping();
        clientNameMapping.setColumnName("Название фирмы");
        clientNameMapping.setExcelColIndex(2);

        SalaryColumnMapping orderDateMapping = new SalaryColumnMapping();
        orderDateMapping.setColumnName("Дата");
        orderDateMapping.setExcelColIndex(3);

        when(salaryColumnMappingRepository.findAll()).thenReturn(Arrays.asList(
                paidAmountMapping, paymentDateMapping, clientNameMapping, orderDateMapping));

        // When
        List<BankStatementSearchResultDTO> results = fundsReceiptService.search(multipartFile, bankName);

        // Then
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        BankStatementSearchResultDTO result = results.get(0);
        assertTrue(result.isFound());
        assertFalse(result.isProcessed());
        assertEquals(new BigDecimal("100.0"), result.getAmount());
        assertEquals("Manager1", result.getManagerLogin());
    }

    @Test
    void search_ShouldReturnNotFound_WhenNoMatchInSalaryFiles() throws IOException {
        // Given
        String bankName = "TestBank";
        BankStatementSetting bankSetting = new BankStatementSetting();
        bankSetting.setBankName(bankName);
        bankSetting.setStartRow(1);
        bankSetting.setAmountColIndex(0);
        bankSetting.setDateColIndex(1);
        bankSetting.setClientNameColIndex(2);
        bankSetting.setDateFormat(BANK_DATE_FORMAT);

        when(bankStatementSettingRepository.findByBankName(bankName)).thenReturn(Optional.of(bankSetting));

        // Create bank statement file
        File bankFile = File.createTempFile("bank", ".xlsx");
        try (Workbook bankWorkbook = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(bankFile)) {
            Sheet bankSheet = bankWorkbook.createSheet();
            Row bankRow = bankSheet.createRow(1);
            bankRow.createCell(0).setCellValue(100.0);
            Cell dateCell = bankRow.createCell(1);
            dateCell.setCellValue(LocalDate.now().format(DateTimeFormatter.ofPattern(bankSetting.getDateFormat())));
            bankRow.createCell(2).setCellValue("Client A");

            bankWorkbook.write(fos);
        }
        MockMultipartFile multipartFile = new MockMultipartFile("file", "bank.xlsx", 
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 
                java.nio.file.Files.readAllBytes(bankFile.toPath()));

        UserDTO user = UserDTO.builder().login("Manager1").build();
        when(userService.getAllUsersSortedByLogin()).thenReturn(Collections.singletonList(user));

        SalaryParseSetting parseSetting = new SalaryParseSetting();
        parseSetting.setStartRow(1);
        when(salaryParseSettingRepository.findAll()).thenReturn(Collections.singletonList(parseSetting));

        SalaryColumnMapping paidAmountMapping = new SalaryColumnMapping();
        paidAmountMapping.setColumnName("Сумма заказа");
        paidAmountMapping.setExcelColIndex(0);

        SalaryColumnMapping paymentDateMapping = new SalaryColumnMapping();
        paymentDateMapping.setColumnName("Дата прихода");
        paymentDateMapping.setExcelColIndex(1);

        SalaryColumnMapping clientNameMapping = new SalaryColumnMapping();
        clientNameMapping.setColumnName("Название фирмы");
        clientNameMapping.setExcelColIndex(2);

        SalaryColumnMapping orderDateMapping = new SalaryColumnMapping();
        orderDateMapping.setColumnName("Дата");
        orderDateMapping.setExcelColIndex(3);

        when(salaryColumnMappingRepository.findAll()).thenReturn(Arrays.asList(
                paidAmountMapping, paymentDateMapping, clientNameMapping, orderDateMapping));

        // When
        List<BankStatementSearchResultDTO> results = fundsReceiptService.search(multipartFile, bankName);

        // Then
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        BankStatementSearchResultDTO result = results.get(0);
        assertFalse(result.isFound());
        assertFalse(result.isProcessed());
        assertEquals(new BigDecimal("100.0"), result.getAmount());
    }

    @Test
    void search_ShouldReturnMatchesWithProcessedTrue_WhenAlreadyFilledInSalaryFiles() throws IOException {
        // Given
        String bankName = "TestBank";
        BankStatementSetting bankSetting = new BankStatementSetting();
        bankSetting.setBankName(bankName);
        bankSetting.setStartRow(1);
        bankSetting.setAmountColIndex(0);
        bankSetting.setDateColIndex(1);
        bankSetting.setClientNameColIndex(2);
        bankSetting.setDateFormat(BANK_DATE_FORMAT);

        when(bankStatementSettingRepository.findByBankName(bankName)).thenReturn(Optional.of(bankSetting));

        // Create bank statement file
        File bankFile = File.createTempFile("bank", ".xlsx");
        try (Workbook bankWorkbook = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(bankFile)) {
            Sheet bankSheet = bankWorkbook.createSheet();
            Row bankRow = bankSheet.createRow(1);
            bankRow.createCell(0).setCellValue(100.0);
            Cell dateCell = bankRow.createCell(1);
            dateCell.setCellValue(LocalDate.now().format(DateTimeFormatter.ofPattern(bankSetting.getDateFormat())));
            bankRow.createCell(2).setCellValue("Client A");

            bankWorkbook.write(fos);
        }
        MockMultipartFile multipartFile = new MockMultipartFile("file", "bank.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                java.nio.file.Files.readAllBytes(bankFile.toPath()));

        // Create salary file
        File salaryFile = new File(salaryDir, "Manager1.xlsx");
        try (Workbook salaryWorkbook = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(salaryFile)) {
            Sheet salarySheet = salaryWorkbook.createSheet();
            Row salaryRow = salarySheet.createRow(1);
            salaryRow.createCell(0).setCellValue(100.0); // Paid Amount
            salaryRow.createCell(1).setCellValue(LocalDate.now().format(DateTimeFormatter.ofPattern(SALARY_DATE_FORMAT))); // Payment Date (filled)
            salaryRow.createCell(2).setCellValue("Client A"); // Client Name
            Cell orderDateCell = salaryRow.createCell(3); // Order Date
            orderDateCell.setCellValue(LocalDate.now().format(DateTimeFormatter.ofPattern(SALARY_DATE_FORMAT)));

            salaryWorkbook.write(fos);
        }

        UserDTO user = UserDTO.builder().login("Manager1").build();
        when(userService.getAllUsersSortedByLogin()).thenReturn(Collections.singletonList(user));

        SalaryParseSetting parseSetting = new SalaryParseSetting();
        parseSetting.setStartRow(1);
        when(salaryParseSettingRepository.findAll()).thenReturn(Collections.singletonList(parseSetting));

        SalaryColumnMapping paidAmountMapping = new SalaryColumnMapping();
        paidAmountMapping.setColumnName("Сумма заказа");
        paidAmountMapping.setExcelColIndex(0);

        SalaryColumnMapping paymentDateMapping = new SalaryColumnMapping();
        paymentDateMapping.setColumnName("Дата прихода");
        paymentDateMapping.setExcelColIndex(1);

        SalaryColumnMapping clientNameMapping = new SalaryColumnMapping();
        clientNameMapping.setColumnName("Название фирмы");
        clientNameMapping.setExcelColIndex(2);

        SalaryColumnMapping orderDateMapping = new SalaryColumnMapping();
        orderDateMapping.setColumnName("Дата");
        orderDateMapping.setExcelColIndex(3);

        when(salaryColumnMappingRepository.findAll()).thenReturn(Arrays.asList(
                paidAmountMapping, paymentDateMapping, clientNameMapping, orderDateMapping));

        // When
        List<BankStatementSearchResultDTO> results = fundsReceiptService.search(multipartFile, bankName);

        // Then
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        BankStatementSearchResultDTO result = results.get(0);
        assertTrue(result.isFound());
        assertTrue(result.isProcessed());
        assertEquals(new BigDecimal("100.0"), result.getAmount());
    }

    @Test
    void search_ShouldReturnMatchesWithFoundFalse_WhenFoundDifferentAmount() throws IOException {
        // Given
        String bankName = "TestBank";
        BankStatementSetting bankSetting = new BankStatementSetting();
        bankSetting.setBankName(bankName);
        bankSetting.setStartRow(1);
        bankSetting.setAmountColIndex(0);
        bankSetting.setDateColIndex(1);
        bankSetting.setClientNameColIndex(2);
        bankSetting.setDateFormat(BANK_DATE_FORMAT);

        when(bankStatementSettingRepository.findByBankName(bankName)).thenReturn(Optional.of(bankSetting));

        // Create bank statement file
        File bankFile = File.createTempFile("bank", ".xlsx");
        try (Workbook bankWorkbook = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(bankFile)) {
            Sheet bankSheet = bankWorkbook.createSheet();
            Row bankRow = bankSheet.createRow(1);
            bankRow.createCell(0).setCellValue(100.0);
            Cell dateCell = bankRow.createCell(1);
            dateCell.setCellValue(LocalDate.now().format(DateTimeFormatter.ofPattern(bankSetting.getDateFormat())));
            bankRow.createCell(2).setCellValue("Client A");

            bankWorkbook.write(fos);
        }
        MockMultipartFile multipartFile = new MockMultipartFile("file", "bank.xlsx", 
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 
                java.nio.file.Files.readAllBytes(bankFile.toPath()));

        // Create salary file
        File salaryFile = new File(salaryDir, "Manager1.xlsx");
        try (Workbook salaryWorkbook = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(salaryFile)) {
            Sheet salarySheet = salaryWorkbook.createSheet();
            Row salaryRow = salarySheet.createRow(1);
            salaryRow.createCell(0).setCellValue(200.0); // Paid Amount (different)
            salaryRow.createCell(1); // Payment Date (empty)
            salaryRow.createCell(2).setCellValue("Client A"); // Client Name
            Cell orderDateCell = salaryRow.createCell(3); // Order Date
            orderDateCell.setCellValue(LocalDate.now().format(DateTimeFormatter.ofPattern(SALARY_DATE_FORMAT)));

            salaryWorkbook.write(fos);
        }

        UserDTO user = UserDTO.builder().login("Manager1").build();
        when(userService.getAllUsersSortedByLogin()).thenReturn(Collections.singletonList(user));

        SalaryParseSetting parseSetting = new SalaryParseSetting();
        parseSetting.setStartRow(1);
        when(salaryParseSettingRepository.findAll()).thenReturn(Collections.singletonList(parseSetting));

        SalaryColumnMapping paidAmountMapping = new SalaryColumnMapping();
        paidAmountMapping.setColumnName("Сумма заказа");
        paidAmountMapping.setExcelColIndex(0);

        SalaryColumnMapping paymentDateMapping = new SalaryColumnMapping();
        paymentDateMapping.setColumnName("Дата прихода");
        paymentDateMapping.setExcelColIndex(1);

        SalaryColumnMapping clientNameMapping = new SalaryColumnMapping();
        clientNameMapping.setColumnName("Название фирмы");
        clientNameMapping.setExcelColIndex(2);

        SalaryColumnMapping orderDateMapping = new SalaryColumnMapping();
        orderDateMapping.setColumnName("Дата");
        orderDateMapping.setExcelColIndex(3);

        when(salaryColumnMappingRepository.findAll()).thenReturn(Arrays.asList(
                paidAmountMapping, paymentDateMapping, clientNameMapping, orderDateMapping));

        // When
        List<BankStatementSearchResultDTO> results = fundsReceiptService.search(multipartFile, bankName);

        // Then
        assertFalse(results.isEmpty());
        // We will receive a not found item since it doesn't match and no matches were found overall
        assertEquals(1, results.size());
        
        BankStatementSearchResultDTO notFound = results.get(0);
        assertFalse(notFound.isFound());
        assertFalse(notFound.isProcessed());
        assertEquals(new BigDecimal("100.0"), notFound.getAmount());
    }

    @Test
    void search_MultipleUsersMatch() throws IOException {
        // Given
        String bankName = "TestBank";
        BankStatementSetting bankSetting = new BankStatementSetting();
        bankSetting.setBankName(bankName);
        bankSetting.setStartRow(1);
        bankSetting.setAmountColIndex(0);
        bankSetting.setDateColIndex(1);
        bankSetting.setClientNameColIndex(2);
        bankSetting.setDateFormat(BANK_DATE_FORMAT);

        when(bankStatementSettingRepository.findByBankName(bankName)).thenReturn(Optional.of(bankSetting));

        // Create bank statement file
        File bankFile = File.createTempFile("bank", ".xlsx");
        try (Workbook bankWorkbook = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(bankFile)) {
            Sheet bankSheet = bankWorkbook.createSheet();
            Row bankRow = bankSheet.createRow(1);
            bankRow.createCell(0).setCellValue(100.0);
            Cell dateCell = bankRow.createCell(1);
            dateCell.setCellValue(LocalDate.now().format(DateTimeFormatter.ofPattern(bankSetting.getDateFormat())));
            bankRow.createCell(2).setCellValue("Client A");

            bankWorkbook.write(fos);
        }
        MockMultipartFile multipartFile = new MockMultipartFile("file", "bank.xlsx", 
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 
                java.nio.file.Files.readAllBytes(bankFile.toPath()));

        // Create salary files
        File salaryFile1 = new File(salaryDir, "Manager1.xlsx");
        try (Workbook salaryWorkbook1 = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(salaryFile1)) {
            Sheet salarySheet1 = salaryWorkbook1.createSheet();
            Row salaryRow1 = salarySheet1.createRow(1);
            salaryRow1.createCell(0).setCellValue(100.0); // Paid Amount
            salaryRow1.createCell(1); // Payment Date (empty)
            salaryRow1.createCell(2).setCellValue("Client A"); // Client Name
            Cell orderDateCell1 = salaryRow1.createCell(3); // Order Date
            orderDateCell1.setCellValue(LocalDate.now().format(DateTimeFormatter.ofPattern(SALARY_DATE_FORMAT)));

            salaryWorkbook1.write(fos);
        }

        File salaryFile2 = new File(salaryDir, "Manager2.xlsx");
        try (Workbook salaryWorkbook2 = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(salaryFile2)) {
            Sheet salarySheet2 = salaryWorkbook2.createSheet();
            Row salaryRow2 = salarySheet2.createRow(1);
            salaryRow2.createCell(0).setCellValue(100.0); // Paid Amount
            salaryRow2.createCell(1); // Payment Date (empty)
            salaryRow2.createCell(2).setCellValue("Client A"); // Client Name
            Cell orderDateCell2 = salaryRow2.createCell(3); // Order Date
            orderDateCell2.setCellValue(LocalDate.now().format(DateTimeFormatter.ofPattern(SALARY_DATE_FORMAT)));

            salaryWorkbook2.write(fos);
        }

        UserDTO user1 = UserDTO.builder().login("Manager1").build();
        UserDTO user2 = UserDTO.builder().login("Manager2").build();
        when(userService.getAllUsersSortedByLogin()).thenReturn(Arrays.asList(user1, user2));

        SalaryParseSetting parseSetting = new SalaryParseSetting();
        parseSetting.setStartRow(1);
        when(salaryParseSettingRepository.findAll()).thenReturn(Collections.singletonList(parseSetting));

        SalaryColumnMapping paidAmountMapping = new SalaryColumnMapping();
        paidAmountMapping.setColumnName("Сумма заказа");
        paidAmountMapping.setExcelColIndex(0);

        SalaryColumnMapping paymentDateMapping = new SalaryColumnMapping();
        paymentDateMapping.setColumnName("Дата прихода");
        paymentDateMapping.setExcelColIndex(1);

        SalaryColumnMapping clientNameMapping = new SalaryColumnMapping();
        clientNameMapping.setColumnName("Название фирмы");
        clientNameMapping.setExcelColIndex(2);

        SalaryColumnMapping orderDateMapping = new SalaryColumnMapping();
        orderDateMapping.setColumnName("Дата");
        orderDateMapping.setExcelColIndex(3);

        when(salaryColumnMappingRepository.findAll()).thenReturn(Arrays.asList(
                paidAmountMapping, paymentDateMapping, clientNameMapping, orderDateMapping));

        // When
        List<BankStatementSearchResultDTO> results = fundsReceiptService.search(multipartFile, bankName);

        // Then
        assertFalse(results.isEmpty());
        assertEquals(2, results.size());
        
        // Since we are using parallelStream, the order is not guaranteed. Sort or check independently
        boolean manager1Found = false;
        boolean manager2Found = false;
        for (BankStatementSearchResultDTO result : results) {
            assertTrue(result.isFound());
            assertFalse(result.isProcessed());
            assertEquals(new BigDecimal("100.0"), result.getAmount());
            if ("Manager1".equals(result.getManagerLogin())) manager1Found = true;
            if ("Manager2".equals(result.getManagerLogin())) manager2Found = true;
        }
        assertTrue(manager1Found);
        assertTrue(manager2Found);
    }

    @Test
    void save_ShouldUpdateSalaryFile() throws IOException {
        // Given
        String fileName = "Manager1.xlsx";
        File salaryFile = new File(salaryDir, fileName);
        try (Workbook salaryWorkbook = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(salaryFile)) {
            Sheet salarySheet = salaryWorkbook.createSheet();
            Row salaryRow = salarySheet.createRow(1);
            salaryRow.createCell(0).setCellValue(100.0); // Paid Amount
            salaryRow.createCell(1); // Payment Date (empty)

            salaryWorkbook.write(fos);
        }

        SalaryColumnMapping paymentDateMapping = new SalaryColumnMapping();
        paymentDateMapping.setColumnName("Дата прихода");
        paymentDateMapping.setExcelColIndex(1);

        when(salaryColumnMappingRepository.findAll()).thenReturn(Collections.singletonList(paymentDateMapping));

        BankStatementSaveRequestDTO request = new BankStatementSaveRequestDTO();
        request.setFileName(fileName);
        request.setRowNumber(1);
        String dateToSave = LocalDate.now().format(DateTimeFormatter.ofPattern(SALARY_DATE_FORMAT));
        request.setDate(dateToSave);

        // When
        fundsReceiptService.save(request);

        // Then
        try (Workbook workbook = new XSSFWorkbook(new java.io.FileInputStream(salaryFile))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row row = sheet.getRow(1);
            Cell cell = row.getCell(1);
            assertNotNull(cell);
            assertEquals(CellType.STRING, cell.getCellType());
            assertEquals(dateToSave, cell.getStringCellValue());
        }
    }

    @Test
    void save_ShouldThrowException_WhenFileNotFound() {
        BankStatementSaveRequestDTO request = new BankStatementSaveRequestDTO();
        request.setFileName("NonExistent.xlsx");
        request.setRowNumber(1);
        request.setDate(LocalDate.now().format(DateTimeFormatter.ofPattern(SALARY_DATE_FORMAT)));

        assertThrows(IllegalArgumentException.class, () -> fundsReceiptService.save(request));
    }
}
