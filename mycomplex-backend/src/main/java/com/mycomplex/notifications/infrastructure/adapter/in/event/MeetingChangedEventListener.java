package com.mycomplex.notifications.infrastructure.adapter.in.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.mycomplex.meetings.domain.event.MeetingChangedEvent;
import com.mycomplex.notifications.application.port.in.NotifyMeetingChangedUseCase;

@Component
public class MeetingChangedEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            MeetingChangedEventListener.class
    );

    private final NotifyMeetingChangedUseCase useCase;

    public MeetingChangedEventListener(
            NotifyMeetingChangedUseCase useCase
    ) {
        this.useCase = useCase;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void on(MeetingChangedEvent event) {
        try {
            useCase.notify(event);
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Could not deliver notifications for meeting {}",
                    event.meetingId(),
                    exception
            );
        }
    }
}
