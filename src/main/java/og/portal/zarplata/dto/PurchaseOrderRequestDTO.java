package og.portal.zarplata.dto;

import java.util.List;

public record PurchaseOrderRequestDTO (
    String currentPath,
    List<String> fileNames
) {
}
