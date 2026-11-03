package com.mycomplex.meetings.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class MeetingTest {

    @Test
    void becomesEligibleForReminderTwentyFourHoursBeforeStart() {
        Instant createdAt = Instant.parse("2026-11-02T15:00:00Z");
        Instant scheduledAt = Instant.parse("2026-11-04T15:00:00Z");

        Meeting meeting = Meeting.schedule(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "General assembly",
                "Annual budget review",
                URI.create("https://meet.google.com/abc-defg-hij"),
                scheduledAt,
                createdAt
        );

        assertThat(meeting.isReminderDue(
                Instant.parse("2026-11-03T14:59:59Z")
        )).isFalse();

        assertThat(meeting.isReminderDue(
                Instant.parse("2026-11-03T15:00:00Z")
        )).isTrue();
    }
}
