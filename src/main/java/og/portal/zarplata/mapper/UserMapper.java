package og.portal.zarplata.mapper;

import og.portal.zarplata.dto.UserDTO;
import og.portal.zarplata.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    UserDTO toDto(User user);
}