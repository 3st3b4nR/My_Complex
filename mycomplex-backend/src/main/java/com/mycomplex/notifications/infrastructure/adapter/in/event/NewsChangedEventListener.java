package com.mycomplex.notifications.infrastructure.adapter.in.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.mycomplex.news.domain.event.NewsChangedEvent;
import com.mycomplex.notifications.application.port.in.NotifyNewsChangedUseCase;

@Component
public class NewsChangedEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            NewsChangedEventListener.class
    );

    private final NotifyNewsChangedUseCase useCase;

    public NewsChangedEventListener(
            NotifyNewsChangedUseCase useCase
    ) {
        this.useCase = useCase;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void on(NewsChangedEvent event) {
        try {
            useCase.notify(event);
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Could not deliver notifications for news {}",
                    event.newsId(),
                    exception
            );
        }
    }
}
