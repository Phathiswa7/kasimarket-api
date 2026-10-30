package com.kasimarket.kasimarket_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling switches on Spring's timer, so methods marked @Scheduled
// (like our job expiry check) run on their own at regular times.
@Configuration
@EnableScheduling
public class SchedulingConfig {
}