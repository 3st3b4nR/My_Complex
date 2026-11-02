package com.mycomplex.news.infrastructure.adapter.out.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.mycomplex.news.application.port.out.PublishNewsEventPort;
import com.mycomplex.news.domain.event.NewsChangedEvent;
import com.mycomplex.news.domain.event.NewsChangedEvent.ChangeType;
import com.mycomplex.news.domain.model.News;

@Component
public class SpringNewsEventAdapter
        implements PublishNewsEventPort {

    private final ApplicationEventPublisher eventPublisher;

    public SpringNewsEventAdapter(
            ApplicationEventPublisher eventPublisher
    ) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void publishCreated(News news) {
        publish(news, ChangeType.CREATED);
    }

    @Override
    public void publishUpdated(News news) {
        publish(news, ChangeType.UPDATED);
    }

    private void publish(
            News news,
            ChangeType changeType
    ) {
        eventPublisher.publishEvent(
                new NewsChangedEvent(
                        news.id(),
                        news.propertyId(),
                        news.updatedById(),
                        news.title(),
                        changeType,
                        news.updatedAt()
                )
        );
    }
}