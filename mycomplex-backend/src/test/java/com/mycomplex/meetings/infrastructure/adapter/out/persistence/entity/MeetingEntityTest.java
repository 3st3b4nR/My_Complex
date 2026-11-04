package com.mycomplex.meetings.infrastructure.adapter.out.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.mycomplex.meetings.domain.model.Meeting;

class MeetingEntityTest {
    @Test
    void exposesPersistenceStateAndAppliesDomainChanges() {
        UUID id = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID editorId = UUID.randomUUID();
        Instant created = Instant.parse("2026-11-01T10:00:00Z");
        Instant scheduled = Instant.parse("2026-12-01T10:00:00Z");
        MeetingEntity entity = new MeetingEntity(
                id, propertyId, creatorId, creatorId, "Original", "Descripción",
                "https://meet.example/old", scheduled, null,
                created, created, null, 2);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.isNew()).isTrue();
        entity.markAsPersisted();
        assertThat(entity.isNew()).isFalse();

        Instant reminder = created.plusSeconds(60);
        Meeting changed = new Meeting(
                id, propertyId, creatorId, editorId, "Actualizada", "Nueva",
                URI.create("https://meet.example/new"), scheduled, reminder,
                created, reminder, reminder, 2);
        entity.applyChanges(changed);

        assertThat(entity.getPropertyId()).isEqualTo(propertyId);
        assertThat(entity.getCreatedById()).isEqualTo(creatorId);
        assertThat(entity.getUpdatedById()).isEqualTo(editorId);
        assertThat(entity.getTitle()).isEqualTo("Actualizada");
        assertThat(entity.getDescription()).isEqualTo("Nueva");
        assertThat(entity.getMeetingUrl()).isEqualTo("https://meet.example/new");
        assertThat(entity.getScheduledAt()).isEqualTo(scheduled);
        assertThat(entity.getReminderSentAt()).isEqualTo(reminder);
        assertThat(entity.getCreatedAt()).isEqualTo(created);
        assertThat(entity.getUpdatedAt()).isEqualTo(reminder);
        assertThat(entity.getDeletedAt()).isEqualTo(reminder);
        assertThat(entity.getVersion()).isEqualTo(2);
    }

    @Test
    void rejectsChangingTheEntityIdentity() {
        Instant now = Instant.now();
        MeetingEntity entity = new MeetingEntity(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Título", "Descripción",
                "https://meet.example/1", now.plusSeconds(3600), null,
                now, now, null, 0);
        Meeting other = Meeting.schedule(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Otro", "Descripción", URI.create("https://meet.example/2"),
                now.plusSeconds(7200), now);

        assertThatThrownBy(() -> entity.applyChanges(other))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id cannot be changed");
    }
}
