package og.portal.zarplata.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TextStyle {
    START("text-start"),
    CENTER("text-center"),
    END("text-end");

    private final String cssClass;
}
