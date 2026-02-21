package og.portal.zarplata.mapper;

import og.portal.zarplata.dto.SalaryColumnMappingDTO;
import og.portal.zarplata.model.SalaryColumnMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SalaryColumnMapper {
    SalaryColumnMappingDTO toDto(SalaryColumnMapping entity);
}