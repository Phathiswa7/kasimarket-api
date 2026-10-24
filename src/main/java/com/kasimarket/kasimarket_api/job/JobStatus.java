package com.kasimarket.kasimarket_api.job;

// The stages a job goes through, from booking to payment.
// Only REQUESTED and CANCELLED are used so far. The others come in later steps.
public enum JobStatus {
    REQUESTED,    // the client booked it and is waiting for a worker
    ACCEPTED,     // a worker accepted it
    EN_ROUTE,     // the worker is travelling to the client
    ARRIVED,      // the worker checked in at the address
    IN_PROGRESS,  // the work has started
    COMPLETED,    // the worker finished and sent a photo
    PAID,         // the client confirmed and the payment was made
    CANCELLED,    // the client or worker cancelled
    EXPIRED       // nobody accepted it in time
}