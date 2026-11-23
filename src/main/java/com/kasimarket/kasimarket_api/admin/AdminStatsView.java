package com.kasimarket.kasimarket_api.admin;

// The numbers on the staff dashboard.
public record AdminStatsView(
        long workersSubmitted,
        long workersApproved,
        long workersRejected,
        long workersSuspended,
        long jobsOpen,            // waiting for a worker
        long jobsLive,            // accepted, on the way, arrived or in progress
        long jobsCompleted,       // finished, waiting for the client to pay
        long jobsPaid,
        long jobsCancelled,
        long jobsExpired,
        long incidentsOpen,
        long incidentsInvestigating,
        long sosOpen,             // open incidents that came from the SOS button
        long paidTotal,           // rand paid by clients so far
        long platformFeeTotal     // rand KasiMarket kept
) {
}
