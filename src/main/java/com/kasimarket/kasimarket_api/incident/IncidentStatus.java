package com.kasimarket.kasimarket_api.incident;

// Where an incident is in the admin's work.
public enum IncidentStatus {
    OPEN,           // just reported, nobody has looked at it yet
    INVESTIGATING,  // an admin is working on it
    RESOLVED;       // finished, with a written explanation

    // The rulebook for an incident's life, like the one for jobs.
    // It can only move forward, and a resolved incident stays resolved.
    public boolean canMoveTo(IncidentStatus next) {
        return switch (this) {
            case OPEN -> next == INVESTIGATING || next == RESOLVED;
            case INVESTIGATING -> next == RESOLVED;
            case RESOLVED -> false;
        };
    }
}
