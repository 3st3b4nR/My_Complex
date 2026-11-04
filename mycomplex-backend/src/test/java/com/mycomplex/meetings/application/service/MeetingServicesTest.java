package com.mycomplex.meetings.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mycomplex.meetings.application.exception.MeetingNotFoundException;
import com.mycomplex.meetings.application.exception.MeetingOperationForbiddenException;
import com.mycomplex.meetings.application.exception.MeetingVersionConflictException;
import com.mycomplex.meetings.application.port.in.CreateMeetingCommand;
import com.mycomplex.meetings.application.port.in.DeleteMeetingCommand;
import com.mycomplex.meetings.application.port.in.UpdateMeetingCommand;
import com.mycomplex.meetings.application.port.out.LoadAuthorizedMeetingOrganizerPort;
import com.mycomplex.meetings.application.port.out.MeetingPersistencePort;
import com.mycomplex.meetings.application.port.out.PublishMeetingEventPort;
import com.mycomplex.meetings.domain.model.Meeting;

class MeetingServicesTest {
    private static final UUID PROPERTY_ID = UUID.randomUUID();
    private static final UUID MEETING_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-03T15:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final URI URL = URI.create("https://meet.example/general");

    private LoadAuthorizedMeetingOrganizerPort organizer;
    private MeetingPersistencePort persistence;
    private PublishMeetingEventPort events;

    @BeforeEach
    void setUp() {
        organizer = mock(LoadAuthorizedMeetingOrganizerPort.class);
        persistence = mock(MeetingPersistencePort.class);
        events = mock(PublishMeetingEventPort.class);
        when(organizer.findActiveAdministratorId(PROPERTY_ID, "auth0|admin"))
                .thenReturn(Optional.of(ADMIN_ID));
        when(persistence.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(persistence.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsAndPublishesAFutureMeeting() {
        CreateMeetingService service = new CreateMeetingService(
                organizer, persistence, events, CLOCK);
        Instant scheduledAt = NOW.plusSeconds(48 * 3600);

        Meeting saved = service.create(new CreateMeetingCommand(
                PROPERTY_ID, "auth0|admin", "Asamblea", "Presupuesto",
                URL, scheduledAt));

        assertThat(saved.scheduledAt()).isEqualTo(scheduledAt);
        assertThat(saved.reminderSentAt()).isNull();
        verify(persistence).save(saved);
        verify(events).publishCreated(saved);
    }

    @Test
    void marksAnImmediateReminderAndRejectsUnauthorizedCreation() {
        CreateMeetingService service = new CreateMeetingService(
                organizer, persistence, events, CLOCK);
        Meeting saved = service.create(new CreateMeetingCommand(
                PROPERTY_ID, "auth0|admin", "Consejo", "Seguimiento",
                URL, NOW.plusSeconds(3600)));
        assertThat(saved.reminderSentAt()).isEqualTo(NOW);

        when(organizer.findActiveAdministratorId(PROPERTY_ID, "auth0|other"))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(new CreateMeetingCommand(
                PROPERTY_ID, "auth0|other", "Consejo", "Seguimiento",
                URL, NOW.plusSeconds(3600))))
                .isInstanceOf(MeetingOperationForbiddenException.class);
    }

    @Test
    void updatesAndPublishesAMeeting() {
        Meeting current = meeting(2, NOW.plusSeconds(72 * 3600));
        when(persistence.loadActiveByIdAndPropertyId(MEETING_ID, PROPERTY_ID))
                .thenReturn(Optional.of(current));
        UpdateMeetingService service = new UpdateMeetingService(
                organizer, persistence, events, CLOCK);

        Meeting saved = service.update(new UpdateMeetingCommand(
                PROPERTY_ID, MEETING_ID, "auth0|admin", "Nuevo título",
                "Nueva descripción", URL, NOW.plusSeconds(3600), 2));

        assertThat(saved.title()).isEqualTo("Nuevo título");
        assertThat(saved.updatedById()).isEqualTo(ADMIN_ID);
        assertThat(saved.reminderSentAt()).isEqualTo(NOW);
        verify(events).publishUpdated(saved);
    }

    @Test
    void rejectsMissingStaleAndUnauthorizedUpdates() {
        UpdateMeetingService service = new UpdateMeetingService(
                organizer, persistence, events, CLOCK);
        UpdateMeetingCommand command = new UpdateMeetingCommand(
                PROPERTY_ID, MEETING_ID, "auth0|admin", "Título", "Descripción",
                URL, NOW.plusSeconds(7200), 0);
        when(persistence.loadActiveByIdAndPropertyId(MEETING_ID, PROPERTY_ID))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(command))
                .isInstanceOf(MeetingNotFoundException.class);

        when(persistence.loadActiveByIdAndPropertyId(MEETING_ID, PROPERTY_ID))
                .thenReturn(Optional.of(meeting(3, NOW.plusSeconds(7200))));
        assertThatThrownBy(() -> service.update(command))
                .isInstanceOf(MeetingVersionConflictException.class);

        when(organizer.findActiveAdministratorId(PROPERTY_ID, "auth0|admin"))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(command))
                .isInstanceOf(MeetingOperationForbiddenException.class);
        verify(events, never()).publishUpdated(any());
    }

    @Test
    void softDeletesAnExistingMeeting() {
        Meeting current = meeting(1, NOW.plusSeconds(7200));
        when(persistence.loadActiveByIdAndPropertyId(MEETING_ID, PROPERTY_ID))
                .thenReturn(Optional.of(current));
        DeleteMeetingService service = new DeleteMeetingService(
                organizer, persistence, CLOCK);

        service.delete(new DeleteMeetingCommand(
                PROPERTY_ID, MEETING_ID, "auth0|admin", 1));

        verify(persistence).update(any(Meeting.class));
    }

    @Test
    void rejectsMissingStaleAndUnauthorizedDeletion() {
        DeleteMeetingService service = new DeleteMeetingService(
                organizer, persistence, CLOCK);
        DeleteMeetingCommand command = new DeleteMeetingCommand(
                PROPERTY_ID, MEETING_ID, "auth0|admin", 0);
        when(persistence.loadActiveByIdAndPropertyId(MEETING_ID, PROPERTY_ID))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(command))
                .isInstanceOf(MeetingNotFoundException.class);

        when(persistence.loadActiveByIdAndPropertyId(MEETING_ID, PROPERTY_ID))
                .thenReturn(Optional.of(meeting(2, NOW.plusSeconds(7200))));
        assertThatThrownBy(() -> service.delete(command))
                .isInstanceOf(MeetingVersionConflictException.class);

        when(organizer.findActiveAdministratorId(PROPERTY_ID, "auth0|admin"))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(command))
                .isInstanceOf(MeetingOperationForbiddenException.class);
    }

    private Meeting meeting(long version, Instant scheduledAt) {
        return new Meeting(
                MEETING_ID, PROPERTY_ID, ADMIN_ID, ADMIN_ID, "Asamblea",
                "Presupuesto", URL, scheduledAt, null,
                NOW.minusSeconds(3600), NOW.minusSeconds(3600), null, version);
    }
}
