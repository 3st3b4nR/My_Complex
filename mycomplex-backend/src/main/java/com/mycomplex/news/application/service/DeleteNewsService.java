package com.mycomplex.news.application.service;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

import com.mycomplex.news.application.exception.NewsNotFoundException;
import com.mycomplex.news.application.exception.NewsPublishingForbiddenException;
import com.mycomplex.news.application.exception.NewsVersionConflictException;
import com.mycomplex.news.application.port.in.DeleteNewsCommand;
import com.mycomplex.news.application.port.in.DeleteNewsUseCase;
import com.mycomplex.news.application.port.out.LoadAuthorizedNewsPublisherPort;
import com.mycomplex.news.application.port.out.LoadNewsPort;
import com.mycomplex.news.application.port.out.ScheduleNewsImageDeletionPort;
import com.mycomplex.news.application.port.out.UpdateNewsPort;
import com.mycomplex.news.domain.model.News;

public final class DeleteNewsService
        implements DeleteNewsUseCase {

    private final LoadAuthorizedNewsPublisherPort publisherPort;
    private final LoadNewsPort loadNewsPort;
    private final UpdateNewsPort updateNewsPort;
    private final ScheduleNewsImageDeletionPort imageDeletionPort;
    private final Clock clock;

    public DeleteNewsService(
            LoadAuthorizedNewsPublisherPort publisherPort,
            LoadNewsPort loadNewsPort,
            UpdateNewsPort updateNewsPort,
            ScheduleNewsImageDeletionPort imageDeletionPort,
            Clock clock
    ) {
        this.publisherPort = Objects.requireNonNull(publisherPort);
        this.loadNewsPort = Objects.requireNonNull(loadNewsPort);
        this.updateNewsPort = Objects.requireNonNull(updateNewsPort);
        this.imageDeletionPort = Objects.requireNonNull(
                imageDeletionPort
        );
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public void delete(DeleteNewsCommand command) {
        Objects.requireNonNull(
                command,
                "command must not be null"
        );

        UUID editorId = publisherPort
                .findActiveAdministratorId(
                        command.propertyId(),
                        command.auth0Subject()
                )
                .orElseThrow(() ->
                        new NewsPublishingForbiddenException(
                                command.propertyId()
                        )
                );

        News currentNews = loadNewsPort
                .loadActiveByIdAndPropertyId(
                        command.newsId(),
                        command.propertyId()
                )
                .orElseThrow(() ->
                        new NewsNotFoundException(command.newsId())
                );

        validateVersion(currentNews, command.version());

        News deletedNews = currentNews.delete(
                editorId,
                clock.instant()
        );

        updateNewsPort.update(deletedNews);
        imageDeletionPort.schedule(currentNews.image());
    }

    private void validateVersion(
            News news,
            long receivedVersion
    ) {
        if (news.version() != receivedVersion) {
            throw new NewsVersionConflictException(
                    news.id(),
                    news.version(),
                    receivedVersion
            );
        }
    }
}
