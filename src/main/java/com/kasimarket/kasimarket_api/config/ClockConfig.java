package com.kasimarket.kasimarket_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// Gives the app one shared "clock". Classes that ask for the time use this clock,
// so tests can replace it with a fake one and jump forward in time instantly.
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        // The real system clock, in UTC time.
        return Clock.systemUTC();
    }
}