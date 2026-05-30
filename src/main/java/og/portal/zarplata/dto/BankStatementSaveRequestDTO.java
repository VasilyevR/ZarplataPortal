package og.portal.zarplata.dto;

import jakarta.validation.constraints.NotBlank;

public record BankStatementSaveRequestDTO(
        String fileName,
        int rowNumber,
        @NotBlank String date
) {
}
