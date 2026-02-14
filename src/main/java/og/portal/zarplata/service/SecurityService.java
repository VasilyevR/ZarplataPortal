package og.portal.zarplata.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import og.portal.zarplata.enums.AppRole;
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
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String KEY_DOMAIN_NAME = "DOMAIN_NAME";

    private final GlobalSettingRepository globalSettingRepository;
    private final RoleMappingRepository roleMappingRepository;

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
            log.debug("SecurityService: User is not authenticated.");
            return false;
        }

        String domainName = globalSettingRepository.findById(KEY_DOMAIN_NAME)
                .map(GlobalSetting::getValue)
                .orElseThrow(() -> new IllegalStateException("Global setting 'DOMAIN_NAME' not found"));

        String adGroup = roleMappingRepository.findByAppRole(role)
                .map(RoleMapping::getAdGroupName)
                .orElse(null);

        if (adGroup == null) {
            log.warn("SecurityService: No AD group mapping found for role: {}", role);
            return false;
        }

        String expectedAuthority = getAuthority(domainName, adGroup);
        String expectedAuthorityNoRole = domainName.toUpperCase() + "\\" + adGroup;

        boolean hasAuthority = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equalsIgnoreCase(expectedAuthority)
                        || a.equalsIgnoreCase(expectedAuthority.replace(ROLE_PREFIX, ""))
                        || a.equalsIgnoreCase(expectedAuthorityNoRole)
                );

        if (hasAuthority) {
            log.info("SecurityService: ACCESS GRANTED. User '{}' has the required authority.", authentication.getName());
        } else {
            log.warn("SecurityService: ACCESS DENIED. User '{}' does NOT have authority '{}'. User authorities: {}", 
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
        return ROLE_PREFIX + domainName.toUpperCase() + "_" + adGroup;
    }
}
