package ph.darch.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import ph.darch.api.config.AppProperties;
import ph.darch.api.entity.Admin;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final AppProperties appProperties;
    private final SecretKey signingKey;

    public JwtService(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.signingKey = Keys.hmacShaKeyFor(
                appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Admin admin) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(appProperties.getJwt().getExpirationSeconds());
        return Jwts.builder()
                .subject(admin.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, String username) {
        try {
            Claims claims = parseClaims(token);
            // Reject tokens issued in the future.
            Date issuedAt = claims.getIssuedAt();
            if (issuedAt != null && issuedAt.after(new Date())) {
                return false;
            }
            boolean notExpired = claims.getExpiration().after(new Date());
            return notExpired && username.equals(claims.getSubject());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
