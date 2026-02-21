package og.portal.zarplata.mapper;

import og.portal.zarplata.dto.SalaryColumnMappingDTO;
import og.portal.zarplata.enums.TextStyle;
import og.portal.zarplata.model.SalaryColumnMapping;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SalaryColumnMapper {
    SalaryColumnMappingDTO toDto(SalaryColumnMapping entity);

    @AfterMapping
    default void setAlignmentClass(SalaryColumnMapping entity, @MappingTarget SalaryColumnMappingDTO dto) {
        if (entity.getAlignment() == null) {
            dto.setAlignmentClass(TextStyle.START.getCssClass());
            return;
        }
        switch (entity.getAlignment()) {
            case CENTER:
                dto.setAlignmentClass(TextStyle.CENTER.getCssClass());
                break;
            case RIGHT:
                dto.setAlignmentClass(TextStyle.END.getCssClass());
                break;
            default:
                dto.setAlignmentClass(TextStyle.START.getCssClass());
        }
    }
}
