package og.portal.zarplata.dto;

public record FileNodeDTO(
    String name,
    boolean isDirectory,
    String path
) {
}
