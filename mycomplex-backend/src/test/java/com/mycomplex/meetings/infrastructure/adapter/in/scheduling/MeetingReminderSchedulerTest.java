package com.mycomplex.meetings.infrastructure.adapter.in.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.mycomplex.meetings.application.port.in.SendMeetingRemindersUseCase;

import io.micrometer.observation.tck.TestObservationRegistry;

class MeetingReminderSchedulerTest {

    @Test
    void recordsSuccessfulReminderBatch() {
        AtomicInteger executions = new AtomicInteger();
        TestObservationRegistry observations = TestObservationRegistry.create();
        MeetingReminderScheduler scheduler = new MeetingReminderScheduler(
                executions::incrementAndGet,
                observations
        );

        scheduler.sendDueReminders();

        assertThat(executions).hasValue(1);
        assertObservation(observations, "success", false);
    }

    @Test
    void recordsFailedReminderBatchWithoutEscapingScheduler() {
        TestObservationRegistry observations = TestObservationRegistry.create();
        SendMeetingRemindersUseCase failingUseCase = () -> {
            throw new IllegalStateException("notification unavailable");
        };
        MeetingReminderScheduler scheduler = new MeetingReminderScheduler(
                failingUseCase,
                observations
        );

        scheduler.sendDueReminders();

        assertObservation(observations, "error", true);
    }

    private void assertObservation(
            TestObservationRegistry observations,
            String outcome,
            boolean hasError
    ) {
        var assertion = observations.assertThat()
                .hasSingleObservationThat()
                .hasNameEqualTo("mycomplex.meeting.reminder.batch")
                .hasLowCardinalityKeyValue("outcome", outcome)
                .hasOnlyKeys("outcome");

        if (hasError) {
            assertion.hasError();
        } else {
            assertion.doesNotHaveError();
        }
    }
}
