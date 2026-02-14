package og.portal.zarplata.repository;

import og.portal.zarplata.enums.AppRole;
import og.portal.zarplata.model.RoleMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleMappingRepository extends JpaRepository<RoleMapping, Long> {
    List<RoleMapping> findByAppRole(AppRole appRole);
}
