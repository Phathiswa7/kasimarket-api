
package com.kasimarket.kasimarket_api.pricing;

// The result of a price calculation: everything a client (or worker) needs to see.
// All money amounts are whole rand, for example 80 means R80.
public record PriceQuote(
        Long taskTypeId,     // which task this price is for
        String taskName,     // for example "Dog walking"
        int hours,           // the hours we are charging for (never below the task minimum)
        boolean urgent,      // true if the client asked for urgent help
        int totalPrice,      // what the client pays
        int platformFee,     // what KasiMarket keeps
        int workerEarning    // what the worker receives (total minus fee)
) {
}