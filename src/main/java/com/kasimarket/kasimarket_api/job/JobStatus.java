package com.kasimarket.kasimarket_api.job;

// The stages a job goes through, from booking to payment.
public enum JobStatus {
    REQUESTED,    // the client booked it and is waiting for a worker
    ACCEPTED,     // a worker accepted it
    EN_ROUTE,     // the worker is travelling to the client
    ARRIVED,      // the worker checked in at the address
    IN_PROGRESS,  // the work has started
    COMPLETED,    // the worker finished and sent a photo
    PAID,         // the client confirmed and the payment was made
    CANCELLED,    // the client or worker cancelled
    EXPIRED;      // nobody accepted it in time

    // Says whether a job in THIS stage is allowed to move to the "next" stage.
    // This is the rulebook for the whole journey. Anything not listed is not allowed.
    public boolean canMoveTo(JobStatus next) {
        return switch (this) {
            // Waiting for a worker: can be accepted, cancelled, or run out of time.
            case REQUESTED -> next == ACCEPTED || next == CANCELLED || next == EXPIRED;
            // Cancelling is allowed any time BEFORE the work starts.
            case ACCEPTED -> next == EN_ROUTE || next == CANCELLED;
            case EN_ROUTE -> next == ARRIVED || next == CANCELLED;
            case ARRIVED -> next == IN_PROGRESS || next == CANCELLED;
            // Once work has started, the only way forward is to finish it.
            case IN_PROGRESS -> next == COMPLETED;
            case COMPLETED -> next == PAID;
            // These are the end of the road: nothing can follow them.
            case PAID, CANCELLED, EXPIRED -> false;
        };
    }
}