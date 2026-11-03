package com.mycomplex.meetings.infrastructure.adapter.out.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.mycomplex.meetings.application.port.out.PublishMeetingEventPort;
import com.mycomplex.meetings.domain.event.MeetingChangedEvent;
import com.mycomplex.meetings.domain.event.MeetingChangedEvent.ChangeType;
import com.mycomplex.meetings.domain.model.Meeting;

@Component
public class SpringMeetingEventAdapter
        implements PublishMeetingEventPort {

    private final ApplicationEventPublisher eventPublisher;

    public SpringMeetingEventAdapter(
            ApplicationEventPublisher eventPublisher
    ) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void publishCreated(Meeting meeting) {
        publish(meeting, ChangeType.CREATED);
    }

    @Override
    public void publishUpdated(Meeting meeting) {
        publish(meeting, ChangeType.UPDATED);
    }

    @Override
    public void publishReminder(Meeting meeting) {
        publish(meeting, ChangeType.REMINDER);
    }

    private void publish(
            Meeting meeting,
            ChangeType changeType
    ) {
        var occurredAt = changeType == ChangeType.REMINDER
                ? meeting.reminderSentAt()
                : meeting.updatedAt();

        eventPublisher.publishEvent(
                new MeetingChangedEvent(
                        meeting.id(),
                        meeting.propertyId(),
                        meeting.title(),
                        meeting.description(),
                        meeting.meetingUrl(),
                        meeting.scheduledAt(),
                        changeType,
                        occurredAt
                )
        );
    }
}
