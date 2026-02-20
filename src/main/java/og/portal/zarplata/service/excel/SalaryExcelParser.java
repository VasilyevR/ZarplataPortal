package og.portal.zarplata.service.excel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SalaryExcelParser {

    public Map<String, BigDecimal> parseSalaryFile(File file) throws IOException {
        Map<String, BigDecimal> salaryData = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header

                String invoiceNumber = ExcelHelperService.getCellStringValue(row.getCell(0));
                if (invoiceNumber == null || invoiceNumber.trim().isEmpty()) {
                    continue;
                }

                BigDecimal amount = ExcelHelperService.getCellBigDecimalValue(row.getCell(1));
                
                salaryData.put(invoiceNumber, amount);
            }
        }
        return salaryData;
    }
}
