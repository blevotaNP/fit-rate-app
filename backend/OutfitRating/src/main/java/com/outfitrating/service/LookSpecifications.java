package com.outfitrating.service;

import com.outfitrating.domain.Category;
import com.outfitrating.domain.Look;
import com.outfitrating.domain.LookType;
import com.outfitrating.domain.Occasion;
import com.outfitrating.domain.Season;
import com.outfitrating.domain.Tag;
import jakarta.persistence.criteria.Join;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Composable filters behind {@code GET /api/looks}. */
public final class LookSpecifications {

    private LookSpecifications() {
    }

    public static Specification<Look> typeIs(LookType type) {
        return type == null ? null : (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Look> categoryIs(Category category) {
        return category == null ? null : (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<Look> seasonIs(Season season) {
        return season == null ? null : (root, query, cb) -> cb.equal(root.get("season"), season);
    }

    public static Specification<Look> occasionIs(Occasion occasion) {
        return occasion == null ? null : (root, query, cb) -> cb.equal(root.get("occasion"), occasion);
    }

    public static Specification<Look> brandSlugIs(String brandSlug) {
        return isBlank(brandSlug) ? null : (root, query, cb) ->
                cb.equal(root.get("brand").get("slug"), brandSlug);
    }

    public static Specification<Look> authorIs(String username) {
        return isBlank(username) ? null : (root, query, cb) ->
                cb.equal(cb.lower(root.get("author").get("username")),
                        username.toLowerCase(Locale.ROOT));
    }

    public static Specification<Look> hasTag(String tagSlug) {
        if (isBlank(tagSlug)) {
            return null;
        }
        return (root, query, cb) -> {
            // A tag join multiplies rows, which would corrupt the page count.
            if (query != null) {
                query.distinct(true);
            }
            Join<Look, Tag> tags = root.join("tags");
            return cb.equal(tags.get("slug"), tagSlug);
        };
    }

    public static Specification<Look> featuredOnly(Boolean featured) {
        return featured == null || !featured
                ? null
                : (root, query, cb) -> cb.isTrue(root.get("featured"));
    }

    public static Specification<Look> scoredAtLeast(Integer minScore) {
        return minScore == null ? null : (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("avgScore"), minScore.doubleValue());
    }

    /** Free-text match across the title, the description and the brand name. */
    public static Specification<Look> matches(String term) {
        if (isBlank(term)) {
            return null;
        }
        String pattern = "%" + term.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(root.join("brand", jakarta.persistence.criteria.JoinType.LEFT)
                        .get("name"), "")), pattern));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
