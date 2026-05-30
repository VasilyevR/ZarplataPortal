package og.portal.zarplata.dto;

import lombok.Builder;

@Builder
public record UserDTO (
    Integer id,
    String login
) {
}