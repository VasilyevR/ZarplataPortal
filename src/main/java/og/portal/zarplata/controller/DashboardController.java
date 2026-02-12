package og.portal.zarplata.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import og.portal.zarplata.model.User;
import og.portal.zarplata.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserRepository userRepository;

    @GetMapping("/")
    public String dashboard(Model model, Authentication authentication) {
        log.info("DashboardController: Handling request for '/'");
        String username = authentication.getName();
        log.info("DashboardController: Authenticated user is '{}'", username);

        User user = userRepository.findByLogin(username).orElse(null);

        if (user != null) {
            model.addAttribute("user", user);
        } else {
            User tempUser = new User();
            tempUser.setLogin(username);
            model.addAttribute("user", tempUser);
            log.info("DashboardController: User '{}' not in DB, proceeding with AD credentials only.", username);
        }

        return "dashboard";
    }
}
