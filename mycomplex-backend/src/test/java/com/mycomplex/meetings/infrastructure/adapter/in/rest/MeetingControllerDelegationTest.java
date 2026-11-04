package com.mycomplex.meetings.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import com.mycomplex.meetings.application.port.in.CreateMeetingUseCase;
import com.mycomplex.meetings.application.port.in.DeleteMeetingCommand;
import com.mycomplex.meetings.application.port.in.DeleteMeetingUseCase;
import com.mycomplex.meetings.application.port.in.ListMeetingsUseCase;
import com.mycomplex.meetings.application.port.in.UpdateMeetingUseCase;
import com.mycomplex.meetings.domain.model.Meeting;
import com.mycomplex.meetings.infrastructure.adapter.in.rest.dto.CreateMeetingRequest;
import com.mycomplex.meetings.infrastructure.adapter.in.rest.dto.MeetingResponse;
import com.mycomplex.meetings.infrastructure.adapter.in.rest.dto.UpdateMeetingRequest;
import com.mycomplex.meetings.infrastructure.adapter.in.rest.mapper.MeetingRestMapper;

class MeetingControllerDelegationTest {
    private CreateMeetingUseCase create;
    private UpdateMeetingUseCase update;
    private DeleteMeetingUseCase delete;
    private ListMeetingsUseCase list;
    private MeetingRestMapper mapper;
    private MeetingController controller;
    private Jwt jwt;
    private UUID propertyId;
    private UUID meetingId;
    private Instant scheduledAt;
    private Meeting meeting;
    private MeetingResponse response;

    @BeforeEach
    void setUp() {
        create = mock(CreateMeetingUseCase.class);
        update = mock(UpdateMeetingUseCase.class);
        delete = mock(DeleteMeetingUseCase.class);
        list = mock(ListMeetingsUseCase.class);
        mapper = mock(MeetingRestMapper.class);
        controller = new MeetingController(create, update, delete, list, mapper);
        jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("auth0|admin");
        propertyId = UUID.randomUUID();
        meetingId = UUID.randomUUID();
        scheduledAt = Instant.parse("2026-12-01T15:00:00Z");
        meeting = Meeting.schedule(
                meetingId, propertyId, UUID.randomUUID(), "Asamblea",
                "Presupuesto", URI.create("https://meet.example/1"),
                scheduledAt, Instant.parse("2026-11-01T15:00:00Z"));
        response = new MeetingResponse(
                meetingId, propertyId, "Asamblea", "Presupuesto",
                URI.create("https://meet.example/1"), scheduledAt,
                meeting.updatedAt(), 0);
        when(mapper.toResponse(meeting)).thenReturn(response);
    }

    @Test
    void listsCreatesUpdatesAndDeletesWithinTheProperty() {
        when(list.list(any())).thenReturn(List.of(meeting));
        when(mapper.toResponses(List.of(meeting))).thenReturn(List.of(response));
        assertThat(controller.list(propertyId, jwt)).containsExactly(response);

        when(create.create(any())).thenReturn(meeting);
        assertThat(controller.create(propertyId, jwt, new CreateMeetingRequest(
                "Asamblea", "Presupuesto", URI.create("https://meet.example/1"),
                scheduledAt))).isEqualTo(response);

        when(update.update(any())).thenReturn(meeting);
        assertThat(controller.update(propertyId, meetingId, jwt,
                new UpdateMeetingRequest(
                        "Asamblea", "Presupuesto",
                        URI.create("https://meet.example/1"), scheduledAt, 0)))
                .isEqualTo(response);

        controller.delete(propertyId, meetingId, jwt, 0);
        verify(delete).delete(new DeleteMeetingCommand(
                propertyId, meetingId, "auth0|admin", 0));
    }
}
