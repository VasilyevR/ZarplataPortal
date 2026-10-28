package og.portal.zarplata.dto;

public record SupplierSettingDTO(
        Long id,
        String title,
        String colorName,
        String colorHex,
        String fileName
) {
}