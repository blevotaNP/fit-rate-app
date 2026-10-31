package com.outfitrating.dto;

/**
 * The five judging criteria on a 1..10 scale plus the composite 0..100 verdict.
 * Null entries mean nobody has rated the look yet.
 */
public record ScoreBreakdownDto(
        Double style,
        Double fit,
        Double color,
        Double originality,
        Double quality,
        Double overall) {

    public static final ScoreBreakdownDto EMPTY =
            new ScoreBreakdownDto(null, null, null, null, null, null);
}
