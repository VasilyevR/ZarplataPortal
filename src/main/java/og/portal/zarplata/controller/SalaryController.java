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
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/salary")
@RequiredArgsConstructor
public class SalaryController {

    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;

    @GetMapping
    public String showSalary(Model model, Authentication authentication) {
        String username = authentication.getName();
        log.info("SalaryController: Request for user '{}'", username);

        User user = userRepository.findByLogin(username).orElse(null);

        if (user == null) {
            log.warn("SalaryController: User '{}' not found in database. Showing empty salary page.", username);
            User tempUser = new User();
            tempUser.setLogin(username);
            
            model.addAttribute("user", tempUser);
            model.addAttribute("invoices", Collections.emptyList());
            model.addAttribute("totalSalary", BigDecimal.ZERO);
            model.addAttribute("dataMissing", true);
            return "salary";
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
        model.addAttribute("dataMissing", false);

        return "salary";
    }
}
