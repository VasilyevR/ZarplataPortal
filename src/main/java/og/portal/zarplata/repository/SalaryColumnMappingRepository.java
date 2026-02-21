package og.portal.zarplata.repository;

import og.portal.zarplata.model.SalaryColumnMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SalaryColumnMappingRepository extends JpaRepository<SalaryColumnMapping, Long> {
    List<SalaryColumnMapping> findByVisibleTrueOrderByExcelColIndexAsc();
}