package ph.darch.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ph.darch.api.entity.Admin;
import ph.darch.api.repository.AdminRepository;
import ph.darch.api.security.JwtService;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AdminRepository adminRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Authenticates a username/password pair. Returns a JWT on success, or {@code null}
     * on failure. The failure path is identical whether the user does not exist or the
     * password is wrong (no user enumeration).
     */
    public String login(String username, String password) {
        Admin admin = adminRepository.findByUsername(username).orElse(null);
        if (admin == null || !passwordEncoder.matches(password, admin.getPasswordHash())) {
            log.warn("Failed login attempt for username '{}'.", username);
            return null;
        }
        return jwtService.generateToken(admin);
    }
}
