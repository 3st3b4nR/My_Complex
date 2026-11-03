package com.mycomplex.meetings.application.port.in;

import java.util.List;

import com.mycomplex.meetings.domain.model.Meeting;

public interface ListMeetingsUseCase {

    List<Meeting> list(ListMeetingsQuery query);
}
