package com.kasimarket.kasimarket_api.auth;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Checks the login lock without waiting for real time to pass:
// we use a fake clock that we can move forward ourselves.
class LoginAttemptLimiterTest {

    // A clock that only moves when we say so.
    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T10:00:00Z");

        void advanceMinutes(long minutes) {
            now = now.plus(Duration.ofMinutes(minutes));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();

    // The same numbers as application.properties: 5 attempts, a 15 minute window, a 15 minute lock.
    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(5, 15, 15, clock);

    // Helper: records a number of wrong passwords for one email.
    private void fail(String email, int times) {
        for (int i = 0; i < times; i++) {
            limiter.recordFailure(email);
        }
    }

    @Test
    void fourWrongPasswordsDoNotLockAnyone() {
        fail("a@example.com", 4);
        assertThatCode(() -> limiter.checkAllowed("a@example.com")).doesNotThrowAnyException();
    }

    @Test
    void theFifthWrongPasswordLocksTheEmail() {
        fail("a@example.com", 5);
        assertThatThrownBy(() -> limiter.checkAllowed("a@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Too many failed login attempts");
    }

    @Test
    void theLockEndsAfterFifteenMinutes() {
        fail("a@example.com", 5);
        clock.advanceMinutes(14);
        assertThatThrownBy(() -> limiter.checkAllowed("a@example.com"))
                .isInstanceOf(ResponseStatusException.class);

        clock.advanceMinutes(2);   // now 16 minutes have passed
        assertThatCode(() -> limiter.checkAllowed("a@example.com")).doesNotThrowAnyException();
    }

    @Test
    void aCorrectLoginForgetsEarlierMistakes() {
        fail("a@example.com", 4);
        limiter.recordSuccess("a@example.com");
        fail("a@example.com", 4);   // counting started again from zero
        assertThatCode(() -> limiter.checkAllowed("a@example.com")).doesNotThrowAnyException();
    }

    @Test
    void oldMistakesOutsideTheWindowDoNotCount() {
        fail("a@example.com", 4);
        clock.advanceMinutes(16);   // the 15 minute counting window has run out
        fail("a@example.com", 1);   // this counts as the FIRST mistake of a new window
        assertThatCode(() -> limiter.checkAllowed("a@example.com")).doesNotThrowAnyException();
    }

    @Test
    void oneEmailBeingLockedDoesNotAffectAnother() {
        fail("a@example.com", 5);
        assertThatCode(() -> limiter.checkAllowed("b@example.com")).doesNotThrowAnyException();
    }

    @Test
    void afterTheLockEndsTheCountStartsFromZeroAgain() {
        fail("a@example.com", 5);
        clock.advanceMinutes(16);
        fail("a@example.com", 4);   // four new mistakes are not enough to lock again
        assertThat(limiterIsOpen("a@example.com")).isTrue();
    }

    // Helper: true when checkAllowed does not throw.
    private boolean limiterIsOpen(String email) {
        try {
            limiter.checkAllowed(email);
            return true;
        } catch (ResponseStatusException e) {
            return false;
        }
    }
}