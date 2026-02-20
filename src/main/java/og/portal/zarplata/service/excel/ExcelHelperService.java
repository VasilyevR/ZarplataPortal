package og.portal.zarplata.service.excel;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;

import java.math.BigDecimal;

@Slf4j
public final class ExcelHelperService {
    private ExcelHelperService() {
        throw new IllegalCallerException("ExcelHelperService can't be instantiated");
    }

    public static String getCellStringValue(Cell cell) {
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
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (IllegalStateException e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case ERROR:
                return "";
            default:
                return "";
        }
    }

    public static BigDecimal getCellBigDecimalValue(Cell cell) {
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

    public static String getCellColorHex(Cell cell, Workbook workbook) {
        CellStyle style = cell.getCellStyle();
        Color color = style.getFillForegroundColorColor();

        if (color == null) {
            return null;
        }

        if (color instanceof XSSFColor) {
            XSSFColor xssfColor = (XSSFColor) color;
            if (xssfColor.isAuto()) return null;
            return xssfColor.getARGBHex();
        }

        return null;
    }
}
