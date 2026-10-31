package com.outfitrating.dto;

import com.outfitrating.domain.Category;
import com.outfitrating.domain.LookType;
import com.outfitrating.domain.Occasion;
import com.outfitrating.domain.Season;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CreateLookRequest(
        @NotBlank @Size(min = 3, max = 120) String title,
        @NotNull LookType type,
        @NotNull Category category,
        Season season,
        Occasion occasion,
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 512) String imageUrl,
        @Size(max = 80) String brandName,
        @PositiveOrZero Integer priceCents,
        @Size(min = 3, max = 3) String currency,
        LocalDate releaseDate,
        @Size(max = 8) List<@Size(min = 2, max = 40) String> tags) {
}
