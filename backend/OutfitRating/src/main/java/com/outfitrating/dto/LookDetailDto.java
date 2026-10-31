package com.outfitrating.dto;

import com.outfitrating.domain.Category;
import com.outfitrating.domain.LookType;
import com.outfitrating.domain.Occasion;
import com.outfitrating.domain.Season;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record LookDetailDto(
        Long id,
        String slug,
        String title,
        LookType type,
        Category category,
        Season season,
        Occasion occasion,
        String description,
        String imageUrl,
        BrandDto brand,
        UserSummaryDto author,
        Integer priceCents,
        String currency,
        LocalDate releaseDate,
        boolean featured,
        int ratingsCount,
        int reviewsCount,
        long commentsCount,
        long favoritesCount,
        ScoreBreakdownDto scores,
        List<TagDto> tags,
        Instant createdAt,
        Boolean favoritedByMe,
        ReviewDto myReview,
        boolean editable) {
}
