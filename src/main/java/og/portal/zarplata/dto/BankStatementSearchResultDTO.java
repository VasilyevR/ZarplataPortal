package og.portal.zarplata.dto;

import java.math.BigDecimal;
import java.util.List;

public record BankStatementSearchResultDTO(
        String date,
        String clientName,
        BigDecimal amount,
        String managerLogin,
        String fileName,
        Integer rowNumber,
        boolean found,
        boolean processed,
        List<String> possibleClients,
        String orderDate,
        String subject
) {
    public BankStatementSearchResultDTO(
            String date,
            String clientName,
            BigDecimal amount,
            boolean found,
            boolean processed,
            String subject
    ) {
        this(
                date,
                clientName,
                amount,
                null,
                null,
                null,
                found,
                processed,
                null,
                null,
                subject
        );
    }
}
