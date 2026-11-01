package com.kasimarket.kasimarket_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

// A guard that exists ONLY in the "prod" profile.
// If any setting is unsafe, it throws an error while the app is starting,
// so the app never begins serving customers with dangerous settings.
// "Fail fast" is better than running quietly with a public password.
@Component
@Profile("prod")
public class ProductionSafetyCheck {

    public ProductionSafetyCheck(@Value("${kasimarket.dev-data.enabled:false}") boolean devData,
                                 @Value("${jwt.secret:}") String jwtSecret,
                                 @Value("${spring.h2.console.enabled:false}") boolean h2Console) {
        List<String> problems = SafetyRules.problems(devData, jwtSecret, h2Console);
        if (!problems.isEmpty()) {
            throw new IllegalStateException(
                    "Refusing to start: unsafe production settings: " + String.join("; ", problems));
        }
    }
}