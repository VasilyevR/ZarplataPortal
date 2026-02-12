package og.portal.zarplata.controller;

import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

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
    @PreAuthorize("@securityService.hasRole('ORDER_GENERATOR')")
    public ResponseEntity<byte[]> generateOrders(@RequestParam("files") MultipartFile[] files) {
        try {
            byte[] zipData = purchaseOrderService.generatePurchaseOrders(files);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "purchase_orders.zip");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(zipData);

        } catch (IOException e) {
            return ResponseEntity.status(500).body(null);
        }
    }
}
