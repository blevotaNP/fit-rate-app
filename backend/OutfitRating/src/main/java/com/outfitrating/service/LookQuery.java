package com.outfitrating.service;

import com.outfitrating.domain.Category;
import com.outfitrating.domain.LookType;
import com.outfitrating.domain.Occasion;
import com.outfitrating.domain.Season;
import org.springframework.data.domain.Sort;

/** Every knob the catalogue endpoint exposes, bundled so the signature stays readable. */
public record LookQuery(
        String q,
        LookType type,
        Category category,
        Season season,
        Occasion occasion,
        String brand,
        String tag,
        String author,
        Boolean featured,
        Integer minScore,
        SortOrder sort) {

    public enum SortOrder {
        NEWEST(Sort.by(Sort.Direction.DESC, "createdAt")),
        OLDEST(Sort.by(Sort.Direction.ASC, "createdAt")),
        TOP_RATED(Sort.by(Sort.Order.desc("avgScore").nullsLast(), Sort.Order.desc("ratingsCount"))),
        MOST_RATED(Sort.by(Sort.Direction.DESC, "ratingsCount").and(Sort.by(Sort.Direction.DESC, "createdAt"))),
        LOWEST_RATED(Sort.by(Sort.Order.asc("avgScore").nullsLast(), Sort.Order.desc("ratingsCount"))),
        ALPHABETICAL(Sort.by(Sort.Direction.ASC, "title"));

        private final Sort sort;

        SortOrder(Sort sort) {
            this.sort = sort;
        }

        public Sort sort() {
            return sort;
        }
    }

    public SortOrder sortOrDefault() {
        return sort == null ? SortOrder.NEWEST : sort;
    }
}
