package og.portal.zarplata.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BankStatementSaveRequestDTO {
    private String fileName;
    private int rowNumber;
    
    @NotBlank
    private String date;
}
