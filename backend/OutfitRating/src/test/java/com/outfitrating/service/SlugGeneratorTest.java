package com.outfitrating.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SlugGeneratorTest {

    private final SlugGenerator slugs = new SlugGenerator();

    @Test
    void lowercasesAndHyphenates() {
        assertThat(slugs.slugify("Blackout Layers, Kreuzberg"))
                .isEqualTo("blackout-layers-kreuzberg");
    }

    @Test
    @DisplayName("accents are folded rather than dropped")
    void stripsDiacritics() {
        assertThat(slugs.slugify("Café Crème Élégant")).isEqualTo("cafe-creme-elegant");
    }

    @Test
    @DisplayName("Cyrillic titles transliterate instead of collapsing to a placeholder")
    void transliteratesCyrillic() {
        assertThat(slugs.slugify("Чёрное пальто"))
                .isEqualTo("chernoe-palto");
    }

    @Test
    void trimsLeadingAndTrailingSeparators() {
        assertThat(slugs.slugify("  ...Denim Jacket!!!  ")).isEqualTo("denim-jacket");
    }

    @Test
    @DisplayName("a title with nothing sluggable still yields a usable slug")
    void fallsBackWhenNothingSurvives() {
        assertThat(slugs.slugify("!!! ??? ***")).isEqualTo("look");
        assertThat(slugs.slugify("")).isEqualTo("look");
        assertThat(slugs.slugify(null)).isEqualTo("look");
    }

    @Test
    void appendsACounterUntilTheSlugIsFree() {
        Set<String> taken = Set.of("denim-jacket", "denim-jacket-2", "denim-jacket-3");

        assertThat(slugs.uniqueSlug("Denim Jacket", taken::contains)).isEqualTo("denim-jacket-4");
    }

    @Test
    void leavesAFreeSlugUnsuffixed() {
        assertThat(slugs.uniqueSlug("Denim Jacket", candidate -> false)).isEqualTo("denim-jacket");
    }

    @Test
    @DisplayName("very long titles are truncated without leaving a trailing hyphen")
    void truncatesLongTitles() {
        String slug = slugs.slugify("word ".repeat(60));

        assertThat(slug.length()).isLessThanOrEqualTo(120);
        assertThat(slug).doesNotEndWith("-");
    }
}
