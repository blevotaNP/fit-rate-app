package com.outfitrating.dto;

import com.outfitrating.domain.Category;
import com.outfitrating.domain.LookType;
import com.outfitrating.domain.Season;
import java.time.Instant;
import java.util.List;

/** Card-sized view used by grids, rails and leaderboards. */
public record LookSummaryDto(
        Long id,
        String slug,
        String title,
        LookType type,
        Category category,
        Season season,
        String imageUrl,
        BrandDto brand,
        UserSummaryDto author,
        Integer priceCents,
        String currency,
        boolean featured,
        int ratingsCount,
        int reviewsCount,
        Double avgScore,
        List<TagDto> tags,
        Instant createdAt,
        Boolean favoritedByMe) {
}
