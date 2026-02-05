package og.portal.zarplata.controller;

import lombok.RequiredArgsConstructor;
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

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;

    @GetMapping("/")
    public String dashboard(Model model, Authentication authentication) {
        String username = authentication.getName();

        User user = userRepository.findByLogin(username).orElse(null);

        if (user == null) {
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

        model.addAttribute("user", user);
        model.addAttribute("invoices", invoices);
        model.addAttribute("totalSalary", totalSalary);

        return "dashboard";
    }
}
