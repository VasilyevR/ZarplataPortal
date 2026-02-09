package og.portal.zarplata.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import og.portal.zarplata.model.Invoice;
import og.portal.zarplata.model.User;
import og.portal.zarplata.repository.InvoiceRepository;
import og.portal.zarplata.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;

    @GetMapping("/")
    public String dashboard(Model model, Authentication authentication) {
        log.info("DashboardController: Handling request for '/'");
        String username = authentication.getName();
        log.info("DashboardController: Authenticated user is '{}'", username);

        User user = userRepository.findByLogin(username).orElse(null);

        if (user == null) {
            log.error("DashboardController: User '{}' not found in database.", username);
            model.addAttribute("error", "User not found in database.");
            return "error";
        }

        List<Invoice> invoices = invoiceRepository.findByUser(user);
        
        BigDecimal totalSalary = BigDecimal.ZERO;
        if (user.getPercent() != null) {
             BigDecimal totalPaidSum = invoices.stream()
                .map(Invoice::getGivenSum)
                .filter(sum -> sum != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
             
             totalSalary = totalPaidSum.multiply(user.getPercent()).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
        }

        log.info("DashboardController: Calculated total salary {} for user '{}'", totalSalary, username);

        model.addAttribute("user", user);
        model.addAttribute("invoices", invoices);
        model.addAttribute("totalSalary", totalSalary);

        return "dashboard";
    }
}
