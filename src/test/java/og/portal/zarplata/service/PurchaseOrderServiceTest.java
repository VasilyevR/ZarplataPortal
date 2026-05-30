package og.portal.zarplata.service;

import og.portal.zarplata.dto.GeneratedFileDTO;
import og.portal.zarplata.dto.NamedInputStreamDTO;
import og.portal.zarplata.model.InvoiceParseSetting;
import og.portal.zarplata.model.SupplierSetting;
import og.portal.zarplata.repository.InvoiceParseSettingRepository;
import og.portal.zarplata.repository.SupplierSettingRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseOrderServiceTest {
    private SupplierSettingRepository supplierSettingRepository;
    private InvoiceParseSettingRepository invoiceParseSettingRepository;
    private PurchaseOrderService service;

    public static final SupplierSetting supplierSetting = new SupplierSetting(1L, "1", "2", "FFFFFFFF", "order.xlsx", true);

    @BeforeEach
    void setUp() {
        supplierSettingRepository = Mockito.mock(SupplierSettingRepository.class);
        invoiceParseSettingRepository = Mockito.mock(InvoiceParseSettingRepository.class);
        service = new PurchaseOrderService(
                supplierSettingRepository,
                invoiceParseSettingRepository
        );
    }

    @Test
    void generatePurchaseOrders() throws IOException {
        var file1InputStream = this.getClass().getResourceAsStream("file1.xlsx");
        var file2InputStream = this.getClass().getResourceAsStream("file2.xlsx");
        
        List<NamedInputStreamDTO> streams = new ArrayList<>();
        streams.add(new NamedInputStreamDTO("file1.xlsx", file1InputStream));
        streams.add(new NamedInputStreamDTO("file2.xlsx", file2InputStream));

        Mockito.when(invoiceParseSettingRepository.findAll())
                .thenReturn(List.of(new InvoiceParseSetting(1L, 0, 1, 3, 7, 0)));
        Mockito.when(supplierSettingRepository.findByIsDefaultTrue()).thenReturn(Optional.of(supplierSetting));
        Mockito.when(supplierSettingRepository.findAll()).thenReturn(List.of(supplierSetting));

        var result = service.processInputStreams(streams);

        assertNotNull(result);
        assertEquals(1, result.size());
        GeneratedFileDTO fileOut = result.get(0);
        assertNotNull(fileOut);
        assertEquals(supplierSetting.getFileName(), fileOut.fileName());
        Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(fileOut.content()));
        assertNotNull(workbook);
        Sheet sheet = workbook.getSheetAt(0);
        Row row = sheet.getRow(0);
        String article = row.getCell(0).getStringCellValue();
        double quantity = row.getCell(1).getNumericCellValue();
        assertEquals("2321", article);
        assertEquals(20, quantity);
    }
}
