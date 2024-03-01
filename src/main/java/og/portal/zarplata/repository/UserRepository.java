package og.portal.zarplata.repository;

import og.portal.zarplata.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
}
