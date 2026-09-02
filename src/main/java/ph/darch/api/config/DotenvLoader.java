package ph.darch.api.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads a {@code .env} file from the working directory into the JVM at startup so a
 * plain {@code ./mvnw spring-boot:run} works without manual exporting.
 *
 * <p>Precedence: a real OS environment variable always wins over a value in {@code .env},
 * so deployed environments (Railway/Render inject real env vars) behave identically and
 * are never overridden by a committed/local {@code .env}.
 */
public final class DotenvLoader {

    private DotenvLoader() {
    }

    public static void load() {
        Path envFile = Paths.get(".env");
        if (!Files.exists(envFile)) {
            return;
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(envFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return;
        }

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();

            // Only apply if not already set as a real environment variable.
            if (System.getenv(key) != null) {
                continue;
            }
            // Strip surrounding quotes.
            if (value.length() >= 2
                    && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) {
                value = value.substring(1, value.length() - 1);
            }
            System.setProperty(key, value);
        }
    }
}
