package com.kasimarket.kasimarket_api.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Checks the vetting part of the production safety guard.
class SafetyRulesVettingTest {

    private static final String GOOD_SECRET = "q7Zr2LmX9vTb4NcY8wKd1HsJ6pFgA3eUoRiB5tMxVzC0nQ";

    @Test
    void productionRefusesToRunWithoutTheVettingRule() {
        assertThat(SafetyRules.problems(false, GOOD_SECRET, false, false)).hasSize(1);
    }

    @Test
    void productionAcceptsTheVettingRule() {
        assertThat(SafetyRules.problems(false, GOOD_SECRET, false, true)).isEmpty();
    }

    @Test
    void theOlderThreeValueCheckStillWorks() {
        assertThat(SafetyRules.problems(false, GOOD_SECRET, false)).isEmpty();
    }
}