package og.portal.zarplata.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import og.portal.zarplata.dto.UserDTO;
import og.portal.zarplata.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserService userService;

    @GetMapping("/")
    public String dashboard(Model model, Authentication authentication) {
        String username = authentication.getName();
        UserDTO user = userService.getUserByLogin(username);
        model.addAttribute("user", user);
        return "dashboard";
    }
}