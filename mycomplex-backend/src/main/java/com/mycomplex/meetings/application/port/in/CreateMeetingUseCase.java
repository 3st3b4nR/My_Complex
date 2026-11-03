package com.mycomplex.meetings.application.port.in;

import com.mycomplex.meetings.domain.model.Meeting;

public interface CreateMeetingUseCase {

    Meeting create(CreateMeetingCommand command);
}
