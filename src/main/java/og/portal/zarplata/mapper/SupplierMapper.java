package og.portal.zarplata.mapper;

import og.portal.zarplata.dto.SupplierSettingDTO;
import og.portal.zarplata.model.SupplierSetting;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SupplierMapper {
    SupplierSettingDTO toDto(SupplierSetting entity);
}