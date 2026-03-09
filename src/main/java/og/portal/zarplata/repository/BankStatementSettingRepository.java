package og.portal.zarplata.repository;

import og.portal.zarplata.model.BankStatementSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankStatementSettingRepository extends JpaRepository<BankStatementSetting, Long> {
    Optional<BankStatementSetting> findByBankName(String bankName);
}
