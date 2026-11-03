package com.mycomplex.meetings.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import com.mycomplex.meetings.application.port.in.SendMeetingRemindersUseCase;
import com.mycomplex.meetings.application.port.out.MeetingPersistencePort;
import com.mycomplex.meetings.application.port.out.PublishMeetingEventPort;
import com.mycomplex.meetings.domain.model.Meeting;

public final class SendMeetingRemindersService
        implements SendMeetingRemindersUseCase {

    private static final Duration REMINDER_ADVANCE =
            Duration.ofHours(24);

    private final MeetingPersistencePort persistencePort;
    private final PublishMeetingEventPort eventPort;
    private final Clock clock;

    public SendMeetingRemindersService(
            MeetingPersistencePort persistencePort,
            PublishMeetingEventPort eventPort,
            Clock clock
    ) {
        this.persistencePort = Objects.requireNonNull(persistencePort);
        this.eventPort = Objects.requireNonNull(eventPort);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public void sendDueReminders() {
        Instant now = clock.instant();
        Instant windowEnd = now.plus(REMINDER_ADVANCE);

        for (Meeting meeting : persistencePort.loadDueForReminder(
                now,
                windowEnd
        )) {
            if (!meeting.isReminderDue(now)) {
                continue;
            }

            Meeting updated = persistencePort.update(
                    meeting.markReminderSent(now)
            );
            eventPort.publishReminder(updated);
        }
    }
}
