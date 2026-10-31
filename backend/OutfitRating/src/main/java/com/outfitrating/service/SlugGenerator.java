package com.outfitrating.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Builds URL-safe, collision-free slugs from free-form titles. */
@Component
public class SlugGenerator {

    private static final Pattern NON_ALNUM = Pattern.compile("[^\\p{Alnum}]+");
    private static final Pattern EDGE_DASHES = Pattern.compile("(^-+)|(-+$)");
    private static final int MAX_BASE_LENGTH = 120;

    /** Transliteration table for Cyrillic titles, which the catalogue accepts. */
    private static final String[][] CYRILLIC = {
            {"\u0430", "a"}, {"\u0431", "b"}, {"\u0432", "v"}, {"\u0433", "g"}, {"\u0434", "d"},
            {"\u0435", "e"}, {"\u0451", "e"}, {"\u0436", "zh"}, {"\u0437", "z"}, {"\u0438", "i"},
            {"\u0439", "y"}, {"\u043a", "k"}, {"\u043b", "l"}, {"\u043c", "m"}, {"\u043d", "n"},
            {"\u043e", "o"}, {"\u043f", "p"}, {"\u0440", "r"}, {"\u0441", "s"}, {"\u0442", "t"},
            {"\u0443", "u"}, {"\u0444", "f"}, {"\u0445", "h"}, {"\u0446", "c"}, {"\u0447", "ch"},
            {"\u0448", "sh"}, {"\u0449", "sch"}, {"\u044a", ""}, {"\u044b", "y"}, {"\u044c", ""},
            {"\u044d", "e"}, {"\u044e", "yu"}, {"\u044f", "ya"},
    };

    public String slugify(String input) {
        String lower = input == null ? "" : input.toLowerCase(Locale.ROOT);
        for (String[] pair : CYRILLIC) {
            lower = lower.replace(pair[0], pair[1]);
        }
        String normalized = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = EDGE_DASHES.matcher(NON_ALNUM.matcher(normalized).replaceAll("-"))
                .replaceAll("");
        if (slug.length() > MAX_BASE_LENGTH) {
            slug = EDGE_DASHES.matcher(slug.substring(0, MAX_BASE_LENGTH)).replaceAll("");
        }
        return slug.isEmpty() ? "look" : slug;
    }

    /** Appends {@code -2}, {@code -3}, ... until {@code isTaken} stops matching. */
    public String uniqueSlug(String input, Predicate<String> isTaken) {
        String base = slugify(input);
        String candidate = base;
        int suffix = 1;
        while (isTaken.test(candidate)) {
            suffix++;
            candidate = base + "-" + suffix;
        }
        return candidate;
    }
}
