package com.outfitrating.repo;

/** Projection carrying every denormalised aggregate of a look, recomputed in one query. */
public interface ScoreAggregate {

    long getRatingsCount();

    Long getReviewsCount();

    Double getAvgScore();

    Double getAvgStyle();

    Double getAvgFit();

    Double getAvgColor();

    Double getAvgOriginality();

    Double getAvgQuality();
}
