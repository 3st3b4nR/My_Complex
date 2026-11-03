package com.mycomplex.meetings.infrastructure.adapter.in.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mycomplex.meetings.application.port.in.CreateMeetingCommand;
import com.mycomplex.meetings.application.port.in.CreateMeetingUseCase;
import com.mycomplex.meetings.application.port.in.DeleteMeetingCommand;
import com.mycomplex.meetings.application.port.in.DeleteMeetingUseCase;
import com.mycomplex.meetings.application.port.in.ListMeetingsQuery;
import com.mycomplex.meetings.application.port.in.ListMeetingsUseCase;
import com.mycomplex.meetings.application.port.in.UpdateMeetingCommand;
import com.mycomplex.meetings.application.port.in.UpdateMeetingUseCase;
import com.mycomplex.meetings.infrastructure.adapter.in.rest.dto.CreateMeetingRequest;
import com.mycomplex.meetings.infrastructure.adapter.in.rest.dto.MeetingResponse;
import com.mycomplex.meetings.infrastructure.adapter.in.rest.dto.UpdateMeetingRequest;
import com.mycomplex.meetings.infrastructure.adapter.in.rest.mapper.MeetingRestMapper;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/properties/{propertyId}/meetings")
public class MeetingController {

    private final CreateMeetingUseCase createUseCase;
    private final UpdateMeetingUseCase updateUseCase;
    private final DeleteMeetingUseCase deleteUseCase;
    private final ListMeetingsUseCase listUseCase;
    private final MeetingRestMapper mapper;

    public MeetingController(
            CreateMeetingUseCase createUseCase,
            UpdateMeetingUseCase updateUseCase,
            DeleteMeetingUseCase deleteUseCase,
            ListMeetingsUseCase listUseCase,
            MeetingRestMapper mapper
    ) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<MeetingResponse> list(
            @PathVariable UUID propertyId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return mapper.toResponses(
                listUseCase.list(
                        new ListMeetingsQuery(
                                propertyId,
                                jwt.getSubject()
                        )
                )
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MeetingResponse create(
            @PathVariable UUID propertyId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateMeetingRequest request
    ) {
        return mapper.toResponse(
                createUseCase.create(
                        new CreateMeetingCommand(
                                propertyId,
                                jwt.getSubject(),
                                request.title(),
                                request.description(),
                                request.meetingUrl(),
                                request.scheduledAt()
                        )
                )
        );
    }

    @PutMapping("/{meetingId}")
    public MeetingResponse update(
            @PathVariable UUID propertyId,
            @PathVariable UUID meetingId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateMeetingRequest request
    ) {
        return mapper.toResponse(
                updateUseCase.update(
                        new UpdateMeetingCommand(
                                propertyId,
                                meetingId,
                                jwt.getSubject(),
                                request.title(),
                                request.description(),
                                request.meetingUrl(),
                                request.scheduledAt(),
                                request.version()
                        )
                )
        );
    }

    @DeleteMapping("/{meetingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID propertyId,
            @PathVariable UUID meetingId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @Min(0) long version
    ) {
        deleteUseCase.delete(
                new DeleteMeetingCommand(
                        propertyId,
                        meetingId,
                        jwt.getSubject(),
                        version
                )
        );
    }
}
