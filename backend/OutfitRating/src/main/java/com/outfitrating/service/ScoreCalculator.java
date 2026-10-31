package com.outfitrating.service;

import com.outfitrating.dto.SaveReviewRequest;
import org.springframework.stereotype.Component;

/**
 * Turns the five 1..10 criteria into the single 0..100 verdict shown on cards and leaderboards.
 *
 * <p>The criteria carry equal weight on purpose: weighting style above fit would quietly encode an
 * editorial opinion into what is meant to be a community score. The scale is stretched to 0..100
 * because a two-digit number reads as a verdict, while "7.4/10" reads as a measurement.
 */
@Component
public class ScoreCalculator {

    public static final short MIN_CRITERION = 1;
    public static final short MAX_CRITERION = 10;

    public short compose(short style, short fit, short color, short originality, short quality) {
        int sum = style + fit + color + originality + quality;
        return (short) Math.round(sum * 100.0 / (5 * MAX_CRITERION));
    }

    public short compose(SaveReviewRequest request) {
        return compose(request.style(), request.fit(), request.color(),
                request.originality(), request.quality());
    }
}
