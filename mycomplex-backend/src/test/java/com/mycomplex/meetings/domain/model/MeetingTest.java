package com.mycomplex.meetings.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void updatesResetsReminderAndSoftDeletesMeeting() {
        Instant now = Instant.parse("2026-11-03T15:00:00Z");
        UUID creator = UUID.randomUUID();
        UUID editor = UUID.randomUUID();
        Meeting original = Meeting.schedule(
                UUID.randomUUID(), UUID.randomUUID(), creator,
                " Asamblea ", " Presupuesto ",
                URI.create("https://meet.example/1"), now.plusSeconds(7200), now);
        Meeting reminded = original.markReminderSent(now.plusSeconds(60));
        assertThat(reminded.reminderSentAt()).isEqualTo(now.plusSeconds(60));
        assertThat(reminded.isReminderDue(now.plusSeconds(3600))).isFalse();

        Meeting updated = reminded.update(
                " Consejo ", " Seguimiento ",
                URI.create("https://meet.example/2"), now.plusSeconds(10800),
                editor, now.plusSeconds(120));
        assertThat(updated.title()).isEqualTo("Consejo");
        assertThat(updated.reminderSentAt()).isNull();
        assertThat(updated.updatedById()).isEqualTo(editor);

        Meeting deleted = updated.delete(editor, now.plusSeconds(180));
        assertThat(deleted.isDeleted()).isTrue();
        assertThat(deleted.isReminderDue(now.plusSeconds(3600))).isFalse();
        assertThatThrownBy(() -> deleted.markReminderSent(now.plusSeconds(200)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validatesScheduleTextVersionAndHttpsUrl() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> Meeting.schedule(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Título", "Descripción", URI.create("http://example.com"),
                now.plusSeconds(3600), now))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Meeting.schedule(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                " ", "Descripción", URI.create("https://example.com"),
                now.plusSeconds(3600), now))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Meeting.schedule(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Título", "Descripción", URI.create("https://example.com"),
                now, now))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
