package ph.darch.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ph.darch.api.config.AppProperties;
import ph.darch.api.entity.Admin;
import ph.darch.api.repository.AdminRepository;

import java.time.Instant;

/**
 * Seeds a single admin account at startup from {@code ADMIN_USERNAME}/{@code ADMIN_PASSWORD}.
 * The password is BCrypt-hashed before storage; an existing admin is never overwritten.
 */
@Component
public class AdminSeedService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeedService.class);

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    public AdminSeedService(AdminRepository adminRepository,
                            PasswordEncoder passwordEncoder,
                            AppProperties appProperties) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
    }

    @Override
    @Transactional
    public void run(String... args) {
        String username = appProperties.getAdmin().getUsername();
        String password = appProperties.getAdmin().getPassword();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.warn("ADMIN_USERNAME/ADMIN_PASSWORD not set; skipping admin seed.");
            return;
        }

        if (adminRepository.existsByUsername(username)) {
            log.info("Admin '{}' already exists; not overwriting.", username);
            return;
        }

        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setCreatedAt(Instant.now());
        adminRepository.save(admin);
        log.info("Seeded admin account '{}'.", username);
    }
}
