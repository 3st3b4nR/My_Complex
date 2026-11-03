package com.mycomplex.meetings.infrastructure.adapter.in.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mycomplex.meetings.application.port.in.SendMeetingRemindersUseCase;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

@Component
public class MeetingReminderScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            MeetingReminderScheduler.class
    );

    private final SendMeetingRemindersUseCase useCase;
    private final ObservationRegistry observations;

    public MeetingReminderScheduler(
            SendMeetingRemindersUseCase useCase,
            ObservationRegistry observations
    ) {
        this.useCase = useCase;
        this.observations = observations;
    }

    @Scheduled(
            fixedDelayString =
                    "${mycomplex.meetings.reminder-interval-ms:60000}"
    )
    public void sendDueReminders() {
        Observation observation = Observation.start(
                "mycomplex.meeting.reminder.batch",
                observations
        );

        try {
            try (Observation.Scope ignored = observation.openScope()) {
                useCase.sendDueReminders();
            }
            observation.lowCardinalityKeyValue("outcome", "success");
        } catch (RuntimeException exception) {
            observation.lowCardinalityKeyValue("outcome", "error");
            observation.error(exception);
            LOGGER.error(
                    "Could not process meeting reminders",
                    exception
            );
        } finally {
            observation.stop();
        }
    }
}
