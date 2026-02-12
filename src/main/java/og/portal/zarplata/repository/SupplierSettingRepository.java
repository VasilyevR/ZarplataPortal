package og.portal.zarplata.repository;

import og.portal.zarplata.model.SupplierSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupplierSettingRepository extends JpaRepository<SupplierSetting, Long> {
    Optional<SupplierSetting> findByColorHexIsNull();
}