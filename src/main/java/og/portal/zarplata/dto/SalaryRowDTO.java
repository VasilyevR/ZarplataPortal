package og.portal.zarplata.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalaryRowDTO {
    private Map<Integer, String> columnValues;
    private Map<Integer, String> columnColors;
}