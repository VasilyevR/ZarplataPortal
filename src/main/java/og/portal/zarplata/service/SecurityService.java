package og.portal.zarplata.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import og.portal.zarplata.model.AppRole;
import og.portal.zarplata.model.GlobalSetting;
import og.portal.zarplata.model.RoleMapping;
import og.portal.zarplata.repository.GlobalSettingRepository;
import og.portal.zarplata.repository.RoleMappingRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service("securityService")
@RequiredArgsConstructor
public class SecurityService {

    private final GlobalSettingRepository globalSettingRepository;
    private final RoleMappingRepository roleMappingRepository;

    private static final String KEY_DOMAIN_NAME = "DOMAIN_NAME";

    @Transactional(readOnly = true)
    public boolean hasRole(String roleName) {
        try {
            AppRole role = AppRole.valueOf(roleName);
            return hasRole(role);
        } catch (IllegalArgumentException e) {
            log.error("Invalid AppRole name: {}", roleName);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean hasRole(AppRole role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String domainName = globalSettingRepository.findById(KEY_DOMAIN_NAME)
                .map(GlobalSetting::getValue)
                .orElseThrow(() -> new IllegalStateException("Global setting 'DOMAIN_NAME' not found"));

        String adGroup = roleMappingRepository.findByAppRole(role)
                .map(RoleMapping::getAdGroupName)
                .orElse(null);

        if (adGroup == null) {
            log.warn("No AD group mapping found for role: {}", role);
            return false;
        }

        String expectedAuthority = getAuthority(domainName, adGroup);
        
        boolean hasAuthority = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equalsIgnoreCase(expectedAuthority) || a.equalsIgnoreCase(expectedAuthority.replace("ROLE_", "")));

        if (!hasAuthority) {
            log.debug("User {} does not have authority {}. User authorities: {}", 
                    authentication.getName(), expectedAuthority, authentication.getAuthorities());
        }

        return hasAuthority;
    }

    @Transactional(readOnly = true)
    public String getAuthorityForRole(AppRole role) {
         String domainName = globalSettingRepository.findById(KEY_DOMAIN_NAME)
                .map(GlobalSetting::getValue)
                .orElseThrow(() -> new IllegalStateException("Global setting 'DOMAIN_NAME' not found"));

        String adGroup = roleMappingRepository.findByAppRole(role)
                .map(RoleMapping::getAdGroupName)
                .orElse("");

        if (adGroup.isEmpty()) return "";

        return getAuthority(domainName, adGroup);
    }

    private static String getAuthority(String domainName, String adGroup) {
        return "ROLE_" + domainName.toUpperCase() + "_" + adGroup;
    }
}
