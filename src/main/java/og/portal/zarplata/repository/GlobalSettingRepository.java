package og.portal.zarplata.repository;

import og.portal.zarplata.model.GlobalSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlobalSettingRepository extends JpaRepository<GlobalSetting, String> {
}
