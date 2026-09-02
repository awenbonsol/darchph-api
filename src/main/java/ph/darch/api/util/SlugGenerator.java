package ph.darch.api.util;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Function;

@Component
public class SlugGenerator {

    private static final int MAX_LENGTH = 240;

    public String slugify(String name) {
        if (name == null) {
            return "";
        }
        String normalized = Normalizer
                .normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = normalized
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s_-]", "")
                .replaceAll("[\\s_-]+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.length() > MAX_LENGTH) {
            slug = slug.substring(0, MAX_LENGTH).replaceAll("-+$", "");
        }
        return slug;
    }

    public String uniqueSlug(String name, Function<String, Boolean> slugExists) {
        String base = slugify(name);
        if (base.isEmpty()) {
            base = "item";
        }
        if (!slugExists.apply(base)) {
            return base;
        }
        for (int i = 2; i < 10_000; i++) {
            String candidate = base + "-" + i;
            if (!slugExists.apply(candidate)) {
                return candidate;
            }
        }
        return base + "-" + System.currentTimeMillis();
    }
}
