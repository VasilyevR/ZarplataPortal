package og.portal.zarplata.service.excel;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public final class OrderGeneratorService {
    private OrderGeneratorService() {
        throw new IllegalCallerException("OrderGeneratorService");
    }

    public static byte[] createOrderFile(Map<String, Integer> articles) throws IOException {
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
}
