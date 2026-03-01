package og.portal.zarplata.dto;

import lombok.Data;
import java.util.List;

@Data
public class PurchaseOrderRequestDTO {
    private String currentPath;
    private List<String> fileNames;
}
