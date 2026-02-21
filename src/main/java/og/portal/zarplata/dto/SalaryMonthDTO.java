package og.portal.zarplata.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalaryMonthDTO {
    private String monthName;
    private Integer year;
    private BigDecimal totalAmount = java.math.BigDecimal.ZERO;
    private List<SalaryRowDTO> rows;
}