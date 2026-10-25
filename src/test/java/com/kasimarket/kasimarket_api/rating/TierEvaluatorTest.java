package com.kasimarket.kasimarket_api.rating;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Checks the rule for earning tier 2. It uses the real thresholds:
// 10 paid jobs AND an average rating of 4.5 or better.
class TierEvaluatorTest {

    private final TierEvaluator evaluator = new TierEvaluator(10, 4.5);

    @Test
    void newWorkersStartAtTier1() {
        assertThat(evaluator.autoTier(0, 0.0)).isEqualTo(1);
    }

    @Test
    void enoughJobsAndAGoodRatingEarnTier2() {
        assertThat(evaluator.autoTier(10, 4.5)).isEqualTo(2);
        assertThat(evaluator.autoTier(25, 4.9)).isEqualTo(2);
    }

    @Test
    void aGreatRatingWithTooFewJobsIsNotEnough() {
        assertThat(evaluator.autoTier(9, 5.0)).isEqualTo(1);
    }

    @Test
    void manyJobsWithAWeakRatingIsNotEnough() {
        assertThat(evaluator.autoTier(30, 4.4)).isEqualTo(1);
    }
}