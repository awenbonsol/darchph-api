package ph.darch.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ph.darch.api.config.DotenvLoader;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class DarchApiApplication {

    public static void main(String[] args) {
        DotenvLoader.load();
        validateMandatoryEnv();
        SpringApplication.run(DarchApiApplication.class, args);
    }

    /**
     * Fails fast (before the Spring context, and therefore before Flyway/datasource
     * auto-configuration) with a clear message naming the missing required environment
     * variables. Runs in {@code main} only, so tests bootstrapped via the test context
     * are unaffected.
     */
    static void validateMandatoryEnv() {
        List<String> required = List.of(
                "DATABASE_URL",
                "DB_USER",
                "DB_PASSWORD",
                "JWT_SECRET",
                "SUPABASE_URL",
                "SUPABASE_SERVICE_KEY",
                "ADMIN_USERNAME",
                "ADMIN_PASSWORD"
        );

        List<String> missing = new ArrayList<>();
        for (String key : required) {
            if (isBlank(resolve(key))) {
                missing.add(key);
            }
        }

        String jwtSecret = resolve("JWT_SECRET");
        if (jwtSecret != null && !jwtSecret.isBlank() && jwtSecret.trim().length() < 32) {
            throw new StartupException(
                    "JWT_SECRET must be at least 32 characters long; current length is "
                            + jwtSecret.trim().length());
        }

        if (!missing.isEmpty()) {
            throw new StartupException(
                    "Required environment variable(s) missing: " + String.join(", ", missing)
                            + ". Set them in your environment or .env before starting.");
        }
    }

    /**
     * Resolves a variable from the OS environment if present, else from system
     * properties (which {@link DotenvLoader} populates from {@code .env}).
     */
    private static String resolve(String key) {
        String fromEnv = System.getenv(key);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        String fromProps = System.getProperty(key);
        return fromProps;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    static class StartupException extends RuntimeException {
        StartupException(String message) {
            super(message);
        }
    }
}
