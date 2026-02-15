package og.portal.zarplata.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import og.portal.zarplata.dto.GeneratedFileDTO;
import og.portal.zarplata.enums.AppRole;
import og.portal.zarplata.model.SupplierSetting;
import og.portal.zarplata.repository.SupplierSettingRepository;
import og.portal.zarplata.service.PurchaseOrderService;
import og.portal.zarplata.service.SecurityService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private static final String SESSION_FILES_KEY = "GENERATED_ORDER_FILES";

    private final SupplierSettingRepository supplierSettingRepository;
    private final PurchaseOrderService purchaseOrderService;
    private final SecurityService securityService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String showPurchaseOrderPage(Model model) {
        List<SupplierSetting> supplierSettings = supplierSettingRepository.findAll();
        model.addAttribute("supplierSettings", supplierSettings);
        
        boolean canGenerate = securityService.hasRole(AppRole.ORDER_GENERATOR);
        model.addAttribute("canGenerate", canGenerate);
        
        return "purchase-orders";
    }

    @PostMapping("/generate")
    @ResponseBody
    @PreAuthorize("@securityService.hasRole('ORDER_GENERATOR')")
    public ResponseEntity<List<String>> generateOrders(@RequestParam("files") MultipartFile[] files, HttpSession session) {
        try {
            List<GeneratedFileDTO> generatedFiles = purchaseOrderService.generatePurchaseOrders(files);

            session.setAttribute(SESSION_FILES_KEY, generatedFiles);

            List<String> fileNames = generatedFiles.stream()
                    .map(GeneratedFileDTO::getFileName)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(fileNames);

        } catch (IOException e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/download/{index}")
    @PreAuthorize("@securityService.hasRole('ORDER_GENERATOR')")
    public ResponseEntity<byte[]> downloadFile(@PathVariable int index, HttpSession session) {
        @SuppressWarnings("unchecked")
        List<GeneratedFileDTO> files = (List<GeneratedFileDTO>) session.getAttribute(SESSION_FILES_KEY);

        if (files == null || index < 0 || index >= files.size()) {
            return ResponseEntity.notFound().build();
        }

        GeneratedFileDTO fileDTO = files.get(index);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        String encodedFileName = URLEncoder.encode(fileDTO.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        headers.setContentDispositionFormData("attachment", encodedFileName);

        return ResponseEntity.ok()
                .headers(headers)
                .body(fileDTO.getContent());
    }
}
