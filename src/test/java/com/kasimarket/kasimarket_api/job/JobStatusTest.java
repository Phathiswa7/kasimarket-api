package com.kasimarket.kasimarket_api.job;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Checks the rulebook for a job's journey, with no database or web server needed.
class JobStatusTest {

    @Test
    void theNormalJourneyIsAllowedStepByStep() {
        assertThat(JobStatus.REQUESTED.canMoveTo(JobStatus.ACCEPTED)).isTrue();
        assertThat(JobStatus.ACCEPTED.canMoveTo(JobStatus.EN_ROUTE)).isTrue();
        assertThat(JobStatus.EN_ROUTE.canMoveTo(JobStatus.ARRIVED)).isTrue();
        assertThat(JobStatus.ARRIVED.canMoveTo(JobStatus.IN_PROGRESS)).isTrue();
        assertThat(JobStatus.IN_PROGRESS.canMoveTo(JobStatus.COMPLETED)).isTrue();
        assertThat(JobStatus.COMPLETED.canMoveTo(JobStatus.PAID)).isTrue();
    }

    @Test
    void stepsCannotBeSkipped() {
        // A worker cannot jump straight from ACCEPTED to IN_PROGRESS.
        assertThat(JobStatus.ACCEPTED.canMoveTo(JobStatus.IN_PROGRESS)).isFalse();
        // And a job cannot be paid before it is completed.
        assertThat(JobStatus.IN_PROGRESS.canMoveTo(JobStatus.PAID)).isFalse();
    }

    @Test
    void cancellingIsAllowedOnlyBeforeTheWorkStarts() {
        assertThat(JobStatus.ACCEPTED.canMoveTo(JobStatus.CANCELLED)).isTrue();
        assertThat(JobStatus.ARRIVED.canMoveTo(JobStatus.CANCELLED)).isTrue();
        // Once the work has started, it cannot be cancelled.
        assertThat(JobStatus.IN_PROGRESS.canMoveTo(JobStatus.CANCELLED)).isFalse();
    }

    @Test
    void finishedJobsCannotMoveAnywhere() {
        // PAID, CANCELLED and EXPIRED are the end of the road.
        for (JobStatus next : JobStatus.values()) {
            assertThat(JobStatus.PAID.canMoveTo(next)).isFalse();
            assertThat(JobStatus.CANCELLED.canMoveTo(next)).isFalse();
            assertThat(JobStatus.EXPIRED.canMoveTo(next)).isFalse();
        }
    }
}