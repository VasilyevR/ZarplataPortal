package og.portal.zarplata.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.GrantedAuthority;
import java.util.stream.Collectors;

@Controller
public class HelloController {
    private static final Logger log = LoggerFactory.getLogger(HelloController.class);

    @GetMapping("/auth")
    public String auth(Model model, final Authentication auth, HttpServletRequest request) {
        String principal = auth.getPrincipal().toString();
        String authorities = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(", "));

        log.info("HelloController: Authenticated user: {}, Remote Address: {}", principal, request.getRemoteAddr());

        model.addAttribute("principal", principal);
        model.addAttribute("authorities", authorities);
        return "main/home";
    }
}
