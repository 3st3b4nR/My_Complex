package com.mycomplex.meetings.infrastructure.adapter.in.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mycomplex.meetings.application.port.in.SendMeetingRemindersUseCase;

@Component
public class MeetingReminderScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            MeetingReminderScheduler.class
    );

    private final SendMeetingRemindersUseCase useCase;

    public MeetingReminderScheduler(
            SendMeetingRemindersUseCase useCase
    ) {
        this.useCase = useCase;
    }

    @Scheduled(
            fixedDelayString =
                    "${mycomplex.meetings.reminder-interval-ms:60000}"
    )
    public void sendDueReminders() {
        try {
            useCase.sendDueReminders();
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Could not process meeting reminders",
                    exception
            );
        }
    }
}
