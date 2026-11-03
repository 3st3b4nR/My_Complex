package com.mycomplex.meetings.infrastructure.adapter.out.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Persistable;

import com.mycomplex.meetings.domain.model.Meeting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

@Entity
@Table(name = "reuniones")
public class MeetingEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "copropiedad_id", nullable = false)
    private UUID propertyId;

    @Column(name = "creado_por_id", nullable = false)
    private UUID createdById;

    @Column(name = "actualizado_por_id", nullable = false)
    private UUID updatedById;

    @Column(name = "titulo", nullable = false, length = 180)
    private String title;

    @Column(name = "descripcion", nullable = false)
    private String description;

    @Column(name = "enlace_reunion", nullable = false, length = 2048)
    private String meetingUrl;

    @Column(name = "programada_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "recordatorio_enviado_at")
    private Instant reminderSentAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Transient
    private boolean newEntity = true;

    protected MeetingEntity() {
    }

    public MeetingEntity(
            UUID id,
            UUID propertyId,
            UUID createdById,
            UUID updatedById,
            String title,
            String description,
            String meetingUrl,
            Instant scheduledAt,
            Instant reminderSentAt,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt,
            long version
    ) {
        this.id = id;
        this.propertyId = propertyId;
        this.createdById = createdById;
        this.updatedById = updatedById;
        this.title = title;
        this.description = description;
        this.meetingUrl = meetingUrl;
        this.scheduledAt = scheduledAt;
        this.reminderSentAt = reminderSentAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.version = version;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markAsPersisted() {
        newEntity = false;
    }

    public UUID getPropertyId() {
        return propertyId;
    }

    public UUID getCreatedById() {
        return createdById;
    }

    public UUID getUpdatedById() {
        return updatedById;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getMeetingUrl() {
        return meetingUrl;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public Instant getReminderSentAt() {
        return reminderSentAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public long getVersion() {
        return version;
    }

    public void applyChanges(Meeting meeting) {
        if (!id.equals(meeting.id())) {
            throw new IllegalArgumentException(
                    "meeting id cannot be changed"
            );
        }

        propertyId = meeting.propertyId();
        createdById = meeting.createdById();
        updatedById = meeting.updatedById();
        title = meeting.title();
        description = meeting.description();
        meetingUrl = meeting.meetingUrl().toString();
        scheduledAt = meeting.scheduledAt();
        reminderSentAt = meeting.reminderSentAt();
        createdAt = meeting.createdAt();
        updatedAt = meeting.updatedAt();
        deletedAt = meeting.deletedAt();
    }

}
