package com.kasimarket.kasimarket_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Public settings that the screens need BEFORE anyone has logged in.
// The most important one: are the demo accounts switched on? That is true only in development
// (kasimarket.dev-data.enabled=true). In production that setting is false, so this page returns
// an empty list, and the startup guard refuses to run production with the setting on.
@RestController
@RequestMapping("/api/v1/public")
public class PublicConfigController {

    // One demo account shown as a button on the login screen.
    public record DemoAccount(String label, String email, String password, String role) {
    }

    // What the screens are told.
    public record PublicConfig(boolean devMode, boolean startCodeRequired, List<DemoAccount> demoAccounts) {
    }

    private final boolean devData;
    private final boolean startCodeRequired;

    public PublicConfigController(@Value("${kasimarket.dev-data.enabled:false}") boolean devData,
                                  @Value("${jobs.require-start-code:false}") boolean startCodeRequired) {
        this.devData = devData;
        this.startCodeRequired = startCodeRequired;
    }

    // GET /api/v1/public/config  (no login needed)
    @GetMapping("/config")
    public PublicConfig config() {
        // These are the same accounts that DevDataLoader creates. Never sent in production.
        List<DemoAccount> demo = devData
                ? List.of(
                        new DemoAccount("Client: Sipho", "sipho@example.com", "Password123", "CLIENT"),
                        new DemoAccount("Worker: Thandi", "thandi@example.com", "Password123", "WORKER"),
                        new DemoAccount("Worker: Zanele", "zanele@example.com", "Password123", "WORKER"),
                        new DemoAccount("Admin (staff)", "admin@kasimarket.com", "Admin12345", "ADMIN"))
                : List.of();
        return new PublicConfig(devData, startCodeRequired, demo);
    }
}
