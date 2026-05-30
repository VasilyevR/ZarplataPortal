package og.portal.zarplata.dto;

import java.io.InputStream;

public record NamedInputStreamDTO (
     String name,
     InputStream inputStream
) {
}
