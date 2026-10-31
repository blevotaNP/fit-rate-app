package com.outfitrating.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ScoreCalculatorTest {

    private final ScoreCalculator calculator = new ScoreCalculator();

    @Test
    @DisplayName("straight tens compose to a perfect hundred")
    void perfectScore() {
        assertThat(calculator.compose((short) 10, (short) 10, (short) 10, (short) 10, (short) 10))
                .isEqualTo((short) 100);
    }

    @Test
    @DisplayName("the floor of the scale is 10, not 0 — a 1 is still an opinion")
    void lowestPossibleScore() {
        assertThat(calculator.compose((short) 1, (short) 1, (short) 1, (short) 1, (short) 1))
                .isEqualTo((short) 10);
    }

    @ParameterizedTest(name = "{0},{1},{2},{3},{4} -> {5}")
    @CsvSource({
            "7, 7, 7, 7, 7, 70",
            "10, 8, 9, 6, 7, 80",
            "5, 5, 5, 5, 5, 50",
            "9, 9, 10, 8, 9, 90",
            // 8+7+7+7+7 = 36 -> 72
            "8, 7, 7, 7, 7, 72",
    })
    void composesTheMeanOntoTheHundredScale(short style, short fit, short color,
                                            short originality, short quality, short expected) {
        assertThat(calculator.compose(style, fit, color, originality, quality)).isEqualTo(expected);
    }

    @Test
    @DisplayName("all five criteria carry equal weight")
    void criteriaAreWeightedEqually() {
        short styleHeavy = calculator.compose((short) 10, (short) 5, (short) 5, (short) 5, (short) 5);
        short qualityHeavy = calculator.compose((short) 5, (short) 5, (short) 5, (short) 5, (short) 10);

        assertThat(styleHeavy).isEqualTo(qualityHeavy);
    }

    @Test
    @DisplayName("a half point rounds up rather than truncating")
    void roundsHalfUp() {
        // 7+7+7+7+8 = 36/50 -> 72 exactly; 7+7+7+8+8 = 37/50 -> 74
        assertThat(calculator.compose((short) 7, (short) 7, (short) 7, (short) 8, (short) 8))
                .isEqualTo((short) 74);
    }
}
