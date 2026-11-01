package com.kasimarket.kasimarket_api.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Checks the "is it safe for the real internet?" rules.
class SafetyRulesTest {

    // A long random-looking secret, like a real one.
    private static final String GOOD_SECRET = "q7Zr2LmX9vTb4NcY8wKd1HsJ6pFgA3eUoRiB5tMxVzC0nQ";

    @Test
    void safeSettingsHaveNoProblems() {
        assertThat(SafetyRules.problems(false, GOOD_SECRET, false)).isEmpty();
    }

    @Test
    void fakeTestAccountsAreRefused() {
        assertThat(SafetyRules.problems(true, GOOD_SECRET, false)).hasSize(1);
    }

    @Test
    void aShortSecretIsRefused() {
        assertThat(SafetyRules.problems(false, "short", false)).hasSize(1);
    }

    @Test
    void aMissingSecretIsRefused() {
        assertThat(SafetyRules.problems(false, null, false)).hasSize(1);
        assertThat(SafetyRules.problems(false, "", false)).hasSize(1);
    }

    @Test
    void thePublicDevelopmentSecretIsRefused() {
        // This is the real secret from our public application.properties.
        String publicSecret = "kasimarket-dev-secret-change-me-before-real-use-0123456789";
        assertThat(SafetyRules.problems(false, publicSecret, false)).hasSize(1);
    }

    @Test
    void theDatabaseConsoleIsRefused() {
        assertThat(SafetyRules.problems(false, GOOD_SECRET, true)).hasSize(1);
    }

    @Test
    void everyProblemIsReportedAtOnce() {
        List<String> problems = SafetyRules.problems(true, "short", true);
        assertThat(problems).hasSize(3);
    }
}