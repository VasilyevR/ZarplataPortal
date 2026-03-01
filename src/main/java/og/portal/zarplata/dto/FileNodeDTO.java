package og.portal.zarplata.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileNodeDTO {
    private String name;
    private boolean isDirectory;
    private String path;
}
