package com.kasimarket.kasimarket_api.incident;

// How serious a problem is. The order matters: the list goes from least to most serious,
// and the admin's list shows the most serious first.
public enum IncidentSeverity {
    LOW,       // a small disagreement or misunderstanding
    MEDIUM,    // poor work, repeated lateness, rude behaviour
    HIGH,      // unsafe behaviour, damage, a no-show
    CRITICAL   // danger to a person. Only the SOS button can create these.
}
