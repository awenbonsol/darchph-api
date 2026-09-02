package ph.darch.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ph.darch.api.entity.Admin;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByUsername(String username);

    boolean existsByUsername(String username);
}
