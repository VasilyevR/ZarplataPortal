package og.portal.zarplata.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.GrantedAuthority;
import java.util.stream.Collectors;

@RestController
public class HelloController {
    @GetMapping("/")
    public String index(final Authentication auth) {
        return String.format("Hello, %s. You have authorities: %s", auth.getPrincipal(),
                auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(", ")));
    }
}
