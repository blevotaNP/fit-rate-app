package com.outfitrating.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Create-or-update payload for the single review a user may leave on a look.
 * Prose is optional: scoring without writing is a first-class action.
 */
public record SaveReviewRequest(
        @NotNull @Min(1) @Max(10) Short style,
        @NotNull @Min(1) @Max(10) Short fit,
        @NotNull @Min(1) @Max(10) Short color,
        @NotNull @Min(1) @Max(10) Short originality,
        @NotNull @Min(1) @Max(10) Short quality,
        @Size(max = 140) String title,
        @Size(max = 5000) String body) {
}
