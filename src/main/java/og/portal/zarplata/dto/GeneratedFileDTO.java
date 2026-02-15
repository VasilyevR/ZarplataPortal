package og.portal.zarplata.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GeneratedFileDTO {
    private String fileName;
    private byte[] content;
}
