package com.mycomplex.meetings.infrastructure.adapter.out.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mycomplex.meetings.infrastructure.adapter.out.persistence.entity.MeetingEntity;

import jakarta.persistence.LockModeType;

public interface MeetingJpaRepository
        extends JpaRepository<MeetingEntity, UUID> {

    Optional<MeetingEntity>
            findByIdAndPropertyIdAndDeletedAtIsNull(
                    UUID id,
                    UUID propertyId
            );

    @Query("""
            SELECT meeting
            FROM MeetingEntity meeting
            WHERE meeting.propertyId = :propertyId
              AND meeting.deletedAt IS NULL
            ORDER BY
              CASE WHEN meeting.scheduledAt >= :now THEN 0 ELSE 1 END,
              CASE WHEN meeting.scheduledAt >= :now
                   THEN meeting.scheduledAt END ASC,
              CASE WHEN meeting.scheduledAt < :now
                   THEN meeting.scheduledAt END DESC
            """)
    List<MeetingEntity> findActiveByPropertyId(
            @Param("propertyId") UUID propertyId,
            @Param("now") Instant now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT meeting
            FROM MeetingEntity meeting
            WHERE meeting.deletedAt IS NULL
              AND meeting.reminderSentAt IS NULL
              AND meeting.scheduledAt > :now
              AND meeting.scheduledAt <= :windowEnd
            ORDER BY meeting.scheduledAt ASC
            """)
    List<MeetingEntity> findDueForReminder(
            @Param("now") Instant now,
            @Param("windowEnd") Instant windowEnd
    );
}
