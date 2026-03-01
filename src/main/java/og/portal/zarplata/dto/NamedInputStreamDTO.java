package og.portal.zarplata.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.InputStream;

@Data
@AllArgsConstructor
public class NamedInputStreamDTO {
    private String name;
    private InputStream inputStream;
}
