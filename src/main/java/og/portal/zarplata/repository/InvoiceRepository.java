package og.portal.zarplata.repository;

import og.portal.zarplata.model.Invoice;
import og.portal.zarplata.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByUser(User user);
    Optional<Invoice> findByNumber(String number);
}
