package ph.darch.api.util;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SlugGeneratorTest {

    private final SlugGenerator generator = new SlugGenerator();

    @Test
    void lowercasesAndReplacesSpacesWithDashes() {
        assertThat(generator.slugify("Personalized Wooden Keychain"))
                .isEqualTo("personalized-wooden-keychain");
    }

    @Test
    void stripsAccentsViaAsciiFolding() {
        assertThat(generator.slugify("Café con Leche y Jalapeño"))
                .isEqualTo("cafe-con-leche-y-jalapeno");
    }

    @Test
    void replacesUnderscoresAndStripsNonAlphanumerics() {
        assertThat(generator.slugify("Hello_World & 100% Fun!"))
                .isEqualTo("hello-world-100-fun");
    }

    @Test
    void trimsLeadingAndTrailingDashes() {
        assertThat(generator.slugify("  --foo bar--  ")).isEqualTo("foo-bar");
    }

    @Test
    void collapsesMultipleSeparators() {
        assertThat(generator.slugify("a   b---c__d")).isEqualTo("a-b-c-d");
    }

    @Test
    void capsAt240Characters() {
        String longName = "x".repeat(500);
        assertThat(generator.slugify(longName)).hasSizeLessThanOrEqualTo(240);
    }

    @Test
    void returnsEmptyForNull() {
        assertThat(generator.slugify(null)).isEmpty();
    }

    @Test
    void uniqueSlugAppendsNumberOnCollision() {
        Set<String> taken = Set.of("keychain", "keychain-2", "keychain-3");
        assertThat(generator.uniqueSlug("keychain", taken::contains)).isEqualTo("keychain-4");
    }

    @Test
    void uniqueSlugReturnsBaseWhenFree() {
        assertThat(generator.uniqueSlug("Wooden Keychain", s -> false))
                .isEqualTo("wooden-keychain");
    }

    @Test
    void uniqueSlugFallsBackWhenAllTaken() {
        assertThat(generator.uniqueSlug("", s -> true)).startsWith("item-");
    }
}
