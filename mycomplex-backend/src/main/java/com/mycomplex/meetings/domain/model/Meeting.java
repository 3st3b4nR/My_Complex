package com.mycomplex.meetings.domain.model;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Meeting(
        UUID id,
        UUID propertyId,
        UUID createdById,
        UUID updatedById,
        String title,
        String description,
        URI meetingUrl,
        Instant scheduledAt,
        Instant reminderSentAt,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        long version
) {

    private static final Duration REMINDER_ADVANCE =
            Duration.ofHours(24);

    public Meeting {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(propertyId, "propertyId must not be null");
        Objects.requireNonNull(createdById, "createdById must not be null");
        Objects.requireNonNull(updatedById, "updatedById must not be null");
        Objects.requireNonNull(meetingUrl, "meetingUrl must not be null");
        Objects.requireNonNull(scheduledAt, "scheduledAt must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");

        title = requireText(title, "title");
        description = requireText(description, "description");
        validateUrl(meetingUrl);

        if (title.length() > 180) {
            throw new IllegalArgumentException(
                    "title must not exceed 180 characters"
            );
        }

        if (version < 0) {
            throw new IllegalArgumentException(
                    "version must not be negative"
            );
        }
    }

    public static Meeting schedule(
            UUID id,
            UUID propertyId,
            UUID creatorId,
            String title,
            String description,
            URI meetingUrl,
            Instant scheduledAt,
            Instant creationTime
    ) {
        if (!scheduledAt.isAfter(creationTime)) {
            throw new IllegalArgumentException(
                    "scheduledAt must be in the future"
            );
        }

        return new Meeting(
                id,
                propertyId,
                creatorId,
                creatorId,
                title,
                description,
                meetingUrl,
                scheduledAt,
                null,
                creationTime,
                creationTime,
                null,
                0
        );
    }

    public Meeting update(
            String newTitle,
            String newDescription,
            URI newMeetingUrl,
            Instant newScheduledAt,
            UUID editorId,
            Instant updateTime
    ) {
        ensureNotDeleted();

        if (!newScheduledAt.isAfter(updateTime)) {
            throw new IllegalArgumentException(
                    "scheduledAt must be in the future"
            );
        }

        boolean scheduleChanged = !scheduledAt.equals(newScheduledAt);

        return new Meeting(
                id,
                propertyId,
                createdById,
                editorId,
                newTitle,
                newDescription,
                newMeetingUrl,
                newScheduledAt,
                scheduleChanged ? null : reminderSentAt,
                createdAt,
                updateTime,
                null,
                version
        );
    }

    public Meeting delete(
            UUID editorId,
            Instant deletionTime
    ) {
        ensureNotDeleted();

        return new Meeting(
                id,
                propertyId,
                createdById,
                editorId,
                title,
                description,
                meetingUrl,
                scheduledAt,
                reminderSentAt,
                createdAt,
                deletionTime,
                deletionTime,
                version
        );
    }

    public Meeting markReminderSent(Instant sentAt) {
        ensureNotDeleted();

        return new Meeting(
                id,
                propertyId,
                createdById,
                updatedById,
                title,
                description,
                meetingUrl,
                scheduledAt,
                sentAt,
                createdAt,
                updatedAt,
                null,
                version
        );
    }

    public boolean isReminderDue(Instant now) {
        Objects.requireNonNull(now, "now must not be null");

        Instant reminderTime = scheduledAt.minus(REMINDER_ADVANCE);

        return reminderSentAt == null
                && deletedAt == null
                && !now.isBefore(reminderTime)
                && now.isBefore(scheduledAt);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    private void ensureNotDeleted() {
        if (isDeleted()) {
            throw new IllegalStateException(
                    "deleted meeting cannot be modified"
            );
        }
    }

    private static String requireText(
            String value,
            String field
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " must not be blank"
            );
        }

        return value.trim();
    }

    private static void validateUrl(URI url) {
        if (!"https".equalsIgnoreCase(url.getScheme())
                || url.getHost() == null) {
            throw new IllegalArgumentException(
                    "meetingUrl must be an absolute HTTPS URL"
            );
        }
    }
}
