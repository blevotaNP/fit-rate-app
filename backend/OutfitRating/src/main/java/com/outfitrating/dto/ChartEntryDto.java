package com.outfitrating.dto;

/** One row of a leaderboard: the look plus the metrics that earned it the position. */
public record ChartEntryDto(
        int rank,
        LookSummaryDto look,
        double periodScore,
        long periodReviews) {
}
