package com.kasimarket.kasimarket_api.pricing;

import com.kasimarket.kasimarket_api.catalog.TaskType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// This class works out the price of a task.
// It does NOT touch the database, so it is easy to test.
// @Component tells Spring: "create one of these and let other classes use it".
@Component
public class PriceCalculator {

    // Urgent jobs cost 30 percent more, because workers must drop everything.
    private static final double URGENT_MULTIPLIER = 1.50;

    // The share KasiMarket keeps, read from application.properties (0.20 = 20 percent).
    private final double commissionRate;

    // @Value reads the setting "pricing.commission-rate" from application.properties.
    public PriceCalculator(@Value("${pricing.commission-rate}") double commissionRate) {
        this.commissionRate = commissionRate;
    }

    // Works out the price for one task.
    public PriceQuote calculate(TaskType task, int requestedHours, boolean urgent) {

        // A booking must be for at least 1 hour.
        if (requestedHours < 1) {
            throw new IllegalArgumentException("Hours must be at least 1.");
        }

        // Rule 1: never charge for fewer hours than the task minimum.
        // Example: babysitting has a 3 hour minimum, so 2 hours is charged as 3.
        int hours = Math.max(requestedHours, task.getMinHours());

        // Rule 2: urgent jobs cost 30 percent more.
        double multiplier = urgent ? URGENT_MULTIPLIER : 1.0;

        // Rule 3: total price = rate per hour x hours x urgency, rounded to whole rand.
        int totalPrice = (int) Math.round(task.getBaseRatePerHour() * hours * multiplier);

        // Rule 4: KasiMarket keeps its share, and the worker gets everything else.
        int platformFee = (int) Math.round(totalPrice * commissionRate);
        int workerEarning = totalPrice - platformFee;

        return new PriceQuote(task.getId(), task.getName(), hours, urgent,
                totalPrice, platformFee, workerEarning);
    }
}