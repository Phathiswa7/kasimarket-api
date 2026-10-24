package com.kasimarket.kasimarket_api.pricing;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// What a client sends when asking "how much will this cost?"
// Example: {"taskTypeId": 7, "hours": 2, "urgent": false}
public record QuoteRequest(

        // Which task. It must be filled in.
        @NotNull Long taskTypeId,

        // How many hours. It must be at least 1.
        @Min(1) int hours,

        // True if the client needs help urgently. If left out, it counts as false.
        boolean urgent
) {
}