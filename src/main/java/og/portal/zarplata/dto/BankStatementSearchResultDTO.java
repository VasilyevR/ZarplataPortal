package og.portal.zarplata.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class BankStatementSearchResultDTO {
    private String date;
    private String clientName;
    private BigDecimal amount;
    private String managerLogin;
    private String fileName;
    private int rowNumber;
    private boolean found;
    private boolean processed;
    private List<String> possibleClients;
    private String orderDate;
    private String subject;
}
