package com.kasimarket.kasimarket_api.catalog;

// What we show clients about a task.
// We use a separate "view" instead of the database class, so we choose exactly
// which fields leave the app (for example, the minimum tier stays inside).
public record TaskTypeView(
        Long id,
        String name,
        String category,
        int baseRatePerHour,   // rand per hour, before extras
        int minHours           // the shortest booking allowed
) {
}