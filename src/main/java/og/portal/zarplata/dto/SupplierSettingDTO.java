package og.portal.zarplata.dto;

import lombok.Builder;

@Builder
public record SupplierSettingDTO(
        Long id,
        String title,
        String colorName,
        String colorHex,
        String fileName
) {
}