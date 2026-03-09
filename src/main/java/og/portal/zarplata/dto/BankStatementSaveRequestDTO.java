package og.portal.zarplata.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class BankStatementSaveRequestDTO {
    private String fileName;
    private int rowNumber;
    private LocalDate date;
}
