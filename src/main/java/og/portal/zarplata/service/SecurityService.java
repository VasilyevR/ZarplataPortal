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

import java.util.List;
import java.util.stream.Collectors;

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

        List<String> adGroups = roleMappingRepository.findByAppRole(role).stream()
                .map(RoleMapping::getAdGroupName)
                .collect(Collectors.toList());

        if (adGroups.isEmpty()) {
            log.warn("SecurityService: No AD group mappings found for role: {}", role);
            return false;
        }

        for (String adGroup : adGroups) {
            String expectedAuthority = (ROLE_PREFIX + domainName + "\\" + adGroup).toUpperCase();
            
            String expectedAuthorityNoRole = (domainName + "\\" + adGroup).toUpperCase();

            String expectedAuthorityUnderscore = (ROLE_PREFIX + domainName + "_" + adGroup).toUpperCase();

            boolean hasAuthority = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(String::toUpperCase)
                    .anyMatch(a -> a.equals(expectedAuthority)
                            || a.equals(expectedAuthorityNoRole)
                            || a.equals(expectedAuthorityUnderscore)
                    );

            if (hasAuthority) {
                log.info("SecurityService: ACCESS GRANTED. User '{}' has the required authority via group '{}'.", authentication.getName(), adGroup);
                return true;
            }
        }

        log.warn("SecurityService: ACCESS DENIED. User '{}' does NOT have any of the required groups for role '{}'. Required groups: {}. User authorities: {}",
                authentication.getName(), role, adGroups, authentication.getAuthorities());

        return false;
    }
}
