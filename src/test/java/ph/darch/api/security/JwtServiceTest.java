package ph.darch.api.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ph.darch.api.config.AppProperties;
import ph.darch.api.entity.Admin;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private Admin admin;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.getJwt().setSecret(
                "test-secret-that-is-at-least-thirty-two-characters-long-123456");
        props.getJwt().setExpirationSeconds(3600);
        jwtService = new JwtService(props);

        admin = new Admin();
        admin.setUsername("admin");
    }

    @Test
    void generatesTokenWithUsernameAsSubject() {
        String token = jwtService.generateToken(admin);
        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("admin");
    }

    @Test
    void validTokenIsRecognized() {
        String token = jwtService.generateToken(admin);
        assertThat(jwtService.isTokenValid(token, "admin")).isTrue();
    }

    @Test
    void wrongUsernameIsRejected() {
        String token = jwtService.generateToken(admin);
        assertThat(jwtService.isTokenValid(token, "someone-else")).isFalse();
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken(admin);
        String tampered = token.substring(0, token.length() - 4) + "XXXX";
        assertThat(jwtService.isTokenValid(tampered, "admin")).isFalse();
    }

    @Test
    void expiredTokenIsRejected() {
        AppProperties props = new AppProperties();
        props.getJwt().setSecret(
                "test-secret-that-is-at-least-thirty-two-characters-long-123456");
        props.getJwt().setExpirationSeconds(-1); // already expired
        JwtService expiring = new JwtService(props);
        String token = expiring.generateToken(admin);
        assertThat(expiring.isTokenValid(token, "admin")).isFalse();
    }

    @Test
    void garbageTokenIsRejected() {
        assertThat(jwtService.isTokenValid("not.a.jwt", "admin")).isFalse();
    }
}
