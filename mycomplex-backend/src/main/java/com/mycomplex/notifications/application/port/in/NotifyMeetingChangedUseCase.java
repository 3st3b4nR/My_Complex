package com.mycomplex.notifications.application.port.in;

import com.mycomplex.meetings.domain.event.MeetingChangedEvent;

public interface NotifyMeetingChangedUseCase {

    void notify(MeetingChangedEvent event);
}
