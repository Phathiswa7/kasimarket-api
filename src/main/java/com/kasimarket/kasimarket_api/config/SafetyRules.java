package com.kasimarket.kasimarket_api.config;

import java.util.ArrayList;
import java.util.List;

// The rules for "is it safe to run this on the real internet?".
// It is a plain class with no Spring in it, so it is easy to test.
public final class SafetyRules {

    // Part of our public development secret. Anything containing it is not a real secret.
    private static final String DEV_SECRET_MARKER = "kasimarket-dev-secret";

    private SafetyRules() {
    }

    // The original check, kept so older code and tests still work.
    // It assumes the vetting rule is fine.
    public static List<String> problems(boolean devDataEnabled,
                                        String jwtSecret,
                                        boolean h2ConsoleEnabled) {
        return problems(devDataEnabled, jwtSecret, h2ConsoleEnabled, true);
    }

    // The full check. Returns a list of problems. An empty list means the settings are safe.
    public static List<String> problems(boolean devDataEnabled,
                                        String jwtSecret,
                                        boolean h2ConsoleEnabled,
                                        boolean vettingRequired) {
        List<String> problems = new ArrayList<>();

        if (devDataEnabled) {
            problems.add("the fake test accounts are switched on "
                    + "(kasimarket.dev-data.enabled must be false)");
        }
        if (jwtSecret == null || jwtSecret.length() < 32) {
            problems.add("the token secret is missing or shorter than 32 characters");
        } else if (jwtSecret.contains(DEV_SECRET_MARKER)) {
            problems.add("the token secret is the public development secret");
        }
        if (h2ConsoleEnabled) {
            problems.add("the H2 database console is switched on");
        }
        if (!vettingRequired) {
            problems.add("workers can be approved without reviewed documents "
                    + "(vetting.required-for-approval must be true)");
        }
        return problems;
    }
}