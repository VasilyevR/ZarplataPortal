package og.portal.zarplata.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import og.portal.zarplata.dto.BankStatementSaveRequestDTO;
import og.portal.zarplata.dto.BankStatementSearchResultDTO;
import og.portal.zarplata.model.BankStatementSetting;
import og.portal.zarplata.repository.BankStatementSettingRepository;
import og.portal.zarplata.service.FundsReceiptService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/funds-receipt")
@RequiredArgsConstructor
public class FundsReceiptController {

    private final FundsReceiptService fundsReceiptService;
    private final BankStatementSettingRepository bankStatementSettingRepository;

    @GetMapping
    @PreAuthorize("hasRole('FUNDS_RECEIVER')")
    public String showPage(Model model) {
        List<String> bankNames = bankStatementSettingRepository.findAll().stream()
                .map(BankStatementSetting::getBankName)
                .collect(Collectors.toList());
        model.addAttribute("bankNames", bankNames);
        return "funds-receipt";
    }

    @PostMapping("/search")
    @ResponseBody
    @PreAuthorize("hasRole('FUNDS_RECEIVER')")
    public ResponseEntity<List<BankStatementSearchResultDTO>> search(
            @RequestParam("file") MultipartFile file,
            @RequestParam("bankName") String bankName) {
        try {
            List<BankStatementSearchResultDTO> results = fundsReceiptService.search(file, bankName);
            return ResponseEntity.ok(results);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/save")
    @ResponseBody
    @PreAuthorize("hasRole('FUNDS_RECEIVER')")
    public ResponseEntity<Void> save(@Valid @RequestBody BankStatementSaveRequestDTO request) {
        try {
            fundsReceiptService.save(request);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
