package ph.darch.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed access to the {@code app.*} configuration bound from environment variables
 * (see {@code application.yml} and {@code project_overview.md} §9).
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Supabase supabase = new Supabase();
    private final Cors cors = new Cors();
    private final Admin admin = new Admin();

    public Jwt getJwt() {
        return jwt;
    }

    public Supabase getSupabase() {
        return supabase;
    }

    public Cors getCors() {
        return cors;
    }

    public Admin getAdmin() {
        return admin;
    }

    public static class Jwt {
        private String secret = "";
        private long expirationSeconds = 14400;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationSeconds() {
            return expirationSeconds;
        }

        public void setExpirationSeconds(long expirationSeconds) {
            this.expirationSeconds = expirationSeconds;
        }
    }

    public static class Supabase {
        private String url = "";
        private String serviceKey = "";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getServiceKey() {
            return serviceKey;
        }

        public void setServiceKey(String serviceKey) {
            this.serviceKey = serviceKey;
        }
    }

    public static class Cors {
        private String allowedOrigins = "";

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Admin {
        private String username = "";
        private String password = "";

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
