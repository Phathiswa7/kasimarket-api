package com.kasimarket.kasimarket_api.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

// What staff send to change a task. Either field may be left out.
// Example: {"active":false}  or  {"baseRatePerHour":85}
public record TaskUpdateRequest(

        // true = clients can book it, false = switched off.
        Boolean active,

        // The new rand per hour. Sensible limits stop typing mistakes like 8 instead of 80.
        @Min(10) @Max(2000) Integer baseRatePerHour
) {
}
