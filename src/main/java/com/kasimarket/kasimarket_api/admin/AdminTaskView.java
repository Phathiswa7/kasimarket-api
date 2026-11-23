package com.kasimarket.kasimarket_api.admin;

// What staff see about each task, including the ones that are switched off.
public record AdminTaskView(
        Long id,
        String name,
        String category,
        int baseRatePerHour,
        int minHours,
        int minTier,
        boolean active
) {
}
