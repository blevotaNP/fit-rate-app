package com.outfitrating.dto;

/** A suggested look together with a short, human-readable justification. */
public record RecommendationDto(
        LookSummaryDto look,
        String reason,
        double affinity) {
}
