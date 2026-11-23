package com.kasimarket.kasimarket_api.incident;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

// What the phone sends with an SOS: where the person is right now.
// Both values may be left out, because in an emergency the SOS must still work
// even if the phone could not find its position.
public record SosRequest(
        @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @DecimalMin("-180") @DecimalMax("180") Double longitude
) {
}
