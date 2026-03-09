package og.portal.zarplata.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class BankStatementSearchResultDTO {
    private LocalDate date;
    private String clientName;
    private BigDecimal amount;
    private String managerLogin;
    private String fileName;
    private int rowNumber;
    private boolean found;
    private List<String> possibleClients;
    private LocalDate orderDate;
}
