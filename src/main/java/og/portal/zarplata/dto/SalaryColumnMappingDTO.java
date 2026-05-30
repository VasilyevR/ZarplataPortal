package og.portal.zarplata.dto;

import og.portal.zarplata.enums.ColumnAlignment;

public record SalaryColumnMappingDTO (
    int excelColIndex,
    String columnName,
    ColumnAlignment alignment,
    String alignmentClass
) {
}
