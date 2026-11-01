package com.kasimarket.kasimarket_api.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

// Slows down password guessing.
// It counts wrong passwords per email. When the count reaches the limit inside
// the time window, that email is locked for a while.
// The counts live in memory: they reset when the app restarts.
@Component
public class LoginAttemptLimiter {

    // What we remember about one email: how many failures, when counting started,
    // and until when it is locked (null = not locked).
    private record State(int failures, Instant windowStart, Instant lockedUntil) {
    }

    // If we remember more emails than this, we clean out the old ones.
    private static final int PURGE_ABOVE = 5000;

    // A map that is safe when many requests arrive at the same moment.
    private final ConcurrentHashMap<String, State> states = new ConcurrentHashMap<>();

    private final int maxAttempts;
    private final Duration window;
    private final Duration lockTime;
    private final Clock clock;

    // @Value reads the three settings from application.properties.
    public LoginAttemptLimiter(@Value("${login.max-attempts}") int maxAttempts,
                               @Value("${login.window-minutes}") long windowMinutes,
                               @Value("${login.lock-minutes}") long lockMinutes,
                               Clock clock) {
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofMinutes(windowMinutes);
        this.lockTime = Duration.ofMinutes(lockMinutes);
        this.clock = clock;
    }

    // Call this BEFORE checking a password. If the email is locked, it throws
    // "429 Too Many Requests", and the password is never even looked at.
    public void checkAllowed(String key) {
        State state = states.get(key);
        Instant now = clock.instant();

        if (state != null && state.lockedUntil() != null && now.isBefore(state.lockedUntil())) {
            // Work out how many minutes are left, rounded up.
            long minutes = Math.max(1,
                    (long) Math.ceil(Duration.between(now, state.lockedUntil()).toSeconds() / 60.0));
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed login attempts. Try again in " + minutes + " minute(s).");
        }
    }

    // Call this after a wrong password (or an unknown email).
    public void recordFailure(String key) {
        Instant now = clock.instant();

        // Keep the memory small: forget emails whose lock or window is over.
        if (states.size() > PURGE_ABOVE) {
            purgeExpired(now);
        }

        // compute() changes one entry in a single safe step.
        states.compute(key, (k, old) -> {

            // Already locked: nothing changes.
            if (old != null && old.lockedUntil() != null && now.isBefore(old.lockedUntil())) {
                return old;
            }

            // Start counting again if we know nothing about this email, a lock just ended,
            // or the counting window has run out.
            boolean startFresh = old == null
                    || old.lockedUntil() != null
                    || !now.isBefore(old.windowStart().plus(window));

            int failures = startFresh ? 1 : old.failures() + 1;
            Instant windowStart = startFresh ? now : old.windowStart();

            // Reached the limit: lock it.
            Instant lockedUntil = failures >= maxAttempts ? now.plus(lockTime) : null;

            return new State(failures, windowStart, lockedUntil);
        });
    }

    // Call this after a correct password: the count starts again from zero.
    public void recordSuccess(String key) {
        states.remove(key);
    }

    // Removes entries that no longer matter.
    private void purgeExpired(Instant now) {
        states.entrySet().removeIf(entry -> {
            State state = entry.getValue();
            if (state.lockedUntil() != null) {
                return !now.isBefore(state.lockedUntil());
            }
            return !now.isBefore(state.windowStart().plus(window));
        });
    }
}