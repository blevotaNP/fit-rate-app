package com.outfitrating.dto;

public record UserStatsDto(
        long looksPublished,
        long ratingsGiven,
        long reviewsWritten,
        long likesReceived,
        long favorites,
        long followers,
        long following,
        double averageScoreGiven) {
}
