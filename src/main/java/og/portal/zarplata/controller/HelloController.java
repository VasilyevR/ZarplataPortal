package og.portal.zarplata.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.GrantedAuthority;
import java.util.stream.Collectors;

@Controller
public class HelloController {
    @GetMapping("/auth")
    public String auth(Model model, final Authentication auth) {
        String principal = auth.getPrincipal().toString();
        String authorities = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(", "));
        model.addAttribute("principal", principal);
        model.addAttribute("authorities", authorities);
        return "main/home";
    }
}
