package com.kasimarket.kasimarket_api.incident;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Checks the rulebook for an incident's life.
class IncidentStatusTest {

    @Test
    void anIncidentMovesForwardStepByStep() {
        assertThat(IncidentStatus.OPEN.canMoveTo(IncidentStatus.INVESTIGATING)).isTrue();
        assertThat(IncidentStatus.INVESTIGATING.canMoveTo(IncidentStatus.RESOLVED)).isTrue();
    }

    @Test
    void anObviousProblemCanBeResolvedStraightAway() {
        assertThat(IncidentStatus.OPEN.canMoveTo(IncidentStatus.RESOLVED)).isTrue();
    }

    @Test
    void anIncidentNeverMovesBackwards() {
        assertThat(IncidentStatus.INVESTIGATING.canMoveTo(IncidentStatus.OPEN)).isFalse();
        assertThat(IncidentStatus.OPEN.canMoveTo(IncidentStatus.OPEN)).isFalse();
    }

    @Test
    void aResolvedIncidentStaysResolved() {
        for (IncidentStatus next : IncidentStatus.values()) {
            assertThat(IncidentStatus.RESOLVED.canMoveTo(next)).isFalse();
        }
    }

    @Test
    void criticalIsTheMostSeriousLevel() {
        // The admin's list relies on this order: LOW, MEDIUM, HIGH, CRITICAL.
        assertThat(IncidentSeverity.CRITICAL.ordinal()).isGreaterThan(IncidentSeverity.HIGH.ordinal());
        assertThat(IncidentSeverity.HIGH.ordinal()).isGreaterThan(IncidentSeverity.MEDIUM.ordinal());
        assertThat(IncidentSeverity.MEDIUM.ordinal()).isGreaterThan(IncidentSeverity.LOW.ordinal());
    }
}
