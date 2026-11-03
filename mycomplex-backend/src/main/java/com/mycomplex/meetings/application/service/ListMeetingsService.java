package com.mycomplex.meetings.application.service;

import java.util.List;
import java.util.Objects;

import com.mycomplex.meetings.application.exception.MeetingViewingForbiddenException;
import com.mycomplex.meetings.application.port.in.ListMeetingsQuery;
import com.mycomplex.meetings.application.port.in.ListMeetingsUseCase;
import com.mycomplex.meetings.application.port.out.LoadAuthorizedMeetingViewerPort;
import com.mycomplex.meetings.application.port.out.MeetingPersistencePort;
import com.mycomplex.meetings.domain.model.Meeting;

public final class ListMeetingsService
        implements ListMeetingsUseCase {

    private final LoadAuthorizedMeetingViewerPort viewerPort;
    private final MeetingPersistencePort persistencePort;

    public ListMeetingsService(
            LoadAuthorizedMeetingViewerPort viewerPort,
            MeetingPersistencePort persistencePort
    ) {
        this.viewerPort = Objects.requireNonNull(viewerPort);
        this.persistencePort = Objects.requireNonNull(persistencePort);
    }

    @Override
    public List<Meeting> list(ListMeetingsQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        viewerPort.findActiveMemberId(
                query.propertyId(),
                query.auth0Subject()
        ).orElseThrow(() ->
                new MeetingViewingForbiddenException(query.propertyId())
        );

        return persistencePort.loadActiveByPropertyId(
                query.propertyId()
        );
    }
}
