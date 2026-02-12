package og.portal.zarplata.repository;

import og.portal.zarplata.model.InvoiceParseSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceParseSettingRepository extends JpaRepository<InvoiceParseSetting, Long> {
}