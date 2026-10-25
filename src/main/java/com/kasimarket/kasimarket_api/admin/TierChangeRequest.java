package com.kasimarket.kasimarket_api.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

// What an admin sends to change a worker's tier.
// Example: {"tier":3,"referencesVerified":true,"enhancedCheckDone":true}
public record TierChangeRequest(

        // The new tier. Admins may set 2 (back to automatic) or 3 (care tasks).
        @Min(2) @Max(3) int tier,

        // The admin confirms the worker's references were checked by a person.
        boolean referencesVerified,

        // The admin confirms the enhanced background check was completed.
        boolean enhancedCheckDone
) {
}
