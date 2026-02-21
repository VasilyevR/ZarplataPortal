package og.portal.zarplata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import og.portal.zarplata.enums.ColumnAlignment;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryColumnMappingDTO {
    private int excelColIndex;
    private String columnName;
    private ColumnAlignment alignment;
    private String alignmentClass;
}
