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
    void setUp() throws IOException {
        salaryDir = tempDir.resolve("salary").toFile();
        salaryDir.mkdirs();
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
        Workbook bankWorkbook = new XSSFWorkbook();
        Sheet bankSheet = bankWorkbook.createSheet();
        Row bankRow = bankSheet.createRow(1);
        bankRow.createCell(0).setCellValue(100.0);
        Cell dateCell = bankRow.createCell(1);
        dateCell.setCellValue(LocalDate.now().format(DateTimeFormatter.ofPattern(bankSetting.getDateFormat())));
        bankRow.createCell(2).setCellValue("Client A");

        File bankFile = File.createTempFile("bank", ".xlsx");
        try (FileOutputStream fos = new FileOutputStream(bankFile)) {
            bankWorkbook.write(fos);
        }
        MockMultipartFile multipartFile = new MockMultipartFile("file", "bank.xlsx", 
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 
                java.nio.file.Files.readAllBytes(bankFile.toPath()));

        // Create salary file
        File salaryFile = new File(salaryDir, "Manager1.xlsx");
        Workbook salaryWorkbook = new XSSFWorkbook();
        Sheet salarySheet = salaryWorkbook.createSheet();
        Row salaryRow = salarySheet.createRow(1);
        salaryRow.createCell(0).setCellValue(100.0); // Paid Amount
        salaryRow.createCell(1); // Payment Date (empty)
        salaryRow.createCell(2).setCellValue("Client A"); // Client Name
        Cell orderDateCell = salaryRow.createCell(3); // Order Date
        orderDateCell.setCellValue(LocalDate.now().format(DateTimeFormatter.ofPattern(SALARY_DATE_FORMAT)));

        try (FileOutputStream fos = new FileOutputStream(salaryFile)) {
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
        Workbook bankWorkbook = new XSSFWorkbook();
        Sheet bankSheet = bankWorkbook.createSheet();
        Row bankRow = bankSheet.createRow(1);
        bankRow.createCell(0).setCellValue(100.0);
        Cell dateCell = bankRow.createCell(1);
        dateCell.setCellValue(LocalDate.now().format(DateTimeFormatter.ofPattern(bankSetting.getDateFormat())));
        bankRow.createCell(2).setCellValue("Client A");

        File bankFile = File.createTempFile("bank", ".xlsx");
        try (FileOutputStream fos = new FileOutputStream(bankFile)) {
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
        assertEquals(new BigDecimal("100.0"), result.getAmount());
    }

    @Test
    void save_ShouldUpdateSalaryFile() throws IOException {
        // Given
        String fileName = "Manager1.xlsx";
        File salaryFile = new File(salaryDir, fileName);
        Workbook salaryWorkbook = new XSSFWorkbook();
        Sheet salarySheet = salaryWorkbook.createSheet();
        Row salaryRow = salarySheet.createRow(1);
        salaryRow.createCell(0).setCellValue(100.0); // Paid Amount
        salaryRow.createCell(1); // Payment Date (empty)

        try (FileOutputStream fos = new FileOutputStream(salaryFile)) {
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
