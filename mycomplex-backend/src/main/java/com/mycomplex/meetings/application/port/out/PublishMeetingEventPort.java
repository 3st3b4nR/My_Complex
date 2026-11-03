package com.mycomplex.meetings.application.port.out;

import com.mycomplex.meetings.domain.model.Meeting;

public interface PublishMeetingEventPort {

    void publishCreated(Meeting meeting);

    void publishUpdated(Meeting meeting);

    void publishReminder(Meeting meeting);
}
