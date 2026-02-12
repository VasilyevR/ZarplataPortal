package og.portal.zarplata.repository;

import og.portal.zarplata.enums.AppRole;
import og.portal.zarplata.model.RoleMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleMappingRepository extends JpaRepository<RoleMapping, Long> {
    Optional<RoleMapping> findByAppRole(AppRole appRole);
}
