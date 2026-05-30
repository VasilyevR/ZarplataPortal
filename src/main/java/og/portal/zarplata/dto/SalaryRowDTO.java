package og.portal.zarplata.dto;

import java.util.HashMap;
import java.util.Map;

public record SalaryRowDTO (
        Map<Integer, String> columnValues,
        Map<Integer, String> columnColors
) {

    public SalaryRowDTO () {
        this(new HashMap<>(), new HashMap<>());
    }

    public void addColumnValue(int columnIndex, String columnValue) {
        columnValues.put(columnIndex, columnValue);
    }

    public void addColumnColor(int columnIndex, String columnColor) {
        columnColors.put(columnIndex, columnColor);
    }
}