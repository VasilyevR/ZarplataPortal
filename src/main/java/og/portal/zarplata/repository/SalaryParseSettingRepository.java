package og.portal.zarplata.repository;

import og.portal.zarplata.model.SalaryParseSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaryParseSettingRepository extends JpaRepository<SalaryParseSetting, Long> {
}
