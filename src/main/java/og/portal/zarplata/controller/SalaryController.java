package og.portal.zarplata.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import og.portal.zarplata.dto.SalaryColumnMappingDTO;
import og.portal.zarplata.dto.SalaryMonthDTO;
import og.portal.zarplata.dto.UserDTO;
import og.portal.zarplata.enums.AppRole;
import og.portal.zarplata.service.SecurityService;
import og.portal.zarplata.service.SalaryMappingService;
import og.portal.zarplata.service.SalaryService;
import og.portal.zarplata.service.UserService;
import og.portal.zarplata.service.util.DataCleaningService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/salary")
@RequiredArgsConstructor
public class SalaryController {

    private final UserService userService;
    private final SalaryService salaryService;
    private final SalaryMappingService salaryMappingService;
    private final SecurityService securityService;

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("@securityService.hasRole('SALARY_READER') or @securityService.hasRole('USER_MANAGER')")
    public String showSalary(Model model, 
                             Authentication authentication, 
                             HttpSession session,
                             @RequestParam(value = "user", required = false) String targetUser) {
        
        String fullUsername = authentication.getName();
        String currentLogin = DataCleaningService.extractLogin(fullUsername);

        boolean isUserManager = securityService.hasRole(AppRole.USER_MANAGER);

        String viewUser = currentLogin;
        if (isUserManager && targetUser != null && !targetUser.isEmpty()) {
            viewUser = targetUser;
        }

        UserDTO user = userService.getUserByLogin(viewUser);
        List<SalaryMonthDTO> salaryData = salaryService.getSalaryData(user.getLogin(), session);
        List<SalaryColumnMappingDTO> visibleColumns = salaryMappingService.getVisibleColumns();

        if (isUserManager) {
            model.addAttribute("users", userService.getAllUsersSortedByLogin());
        }

        model.addAttribute("user", user);
        model.addAttribute("salaryData", salaryData);
        model.addAttribute("columns", visibleColumns);
        model.addAttribute("selectedUsername", viewUser);

        return "salary";
    }
}
