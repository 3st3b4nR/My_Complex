package com.mycomplex.news.application.service;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

import com.mycomplex.news.application.exception.NewsNotFoundException;
import com.mycomplex.news.application.exception.NewsPublishingForbiddenException;
import com.mycomplex.news.application.exception.NewsVersionConflictException;
import com.mycomplex.news.application.port.in.NewsImageUpload;
import com.mycomplex.news.application.port.in.UpdateNewsCommand;
import com.mycomplex.news.application.port.in.UpdateNewsUseCase;
import com.mycomplex.news.application.port.out.LoadAuthorizedNewsPublisherPort;
import com.mycomplex.news.application.port.out.LoadNewsPort;
import com.mycomplex.news.application.port.out.NewsImageStoragePort;
import com.mycomplex.news.application.port.out.PublishNewsEventPort;
import com.mycomplex.news.application.port.out.UpdateNewsPort;
import com.mycomplex.news.domain.model.News;
import com.mycomplex.news.domain.model.NewsImage;

public final class UpdateNewsService
        implements UpdateNewsUseCase {

    private final LoadAuthorizedNewsPublisherPort publisherPort;
    private final LoadNewsPort loadNewsPort;
    private final UpdateNewsPort updateNewsPort;
    private final NewsImageStoragePort imageStoragePort;
    private final PublishNewsEventPort eventPort;
    private final Clock clock;

    public UpdateNewsService(
            LoadAuthorizedNewsPublisherPort publisherPort,
            LoadNewsPort loadNewsPort,
            UpdateNewsPort updateNewsPort,
            NewsImageStoragePort imageStoragePort,
            PublishNewsEventPort eventPort,
            Clock clock
    ) {
        this.publisherPort = Objects.requireNonNull(publisherPort);
        this.loadNewsPort = Objects.requireNonNull(loadNewsPort);
        this.updateNewsPort = Objects.requireNonNull(updateNewsPort);
        this.imageStoragePort = Objects.requireNonNull(
                imageStoragePort
        );
        this.eventPort = Objects.requireNonNull(eventPort);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public News update(UpdateNewsCommand command) {
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

        NewsImage previousImage = currentNews.image();
        NewsImage replacementImage = null;

        try {
            replacementImage = storeReplacementIfPresent(command);

            News updatedNews = currentNews.update(
                    command.title(),
                    command.content(),
                    replacementImage != null
                            ? replacementImage
                            : previousImage,
                    editorId,
                    clock.instant()
            );

            News savedNews = updateNewsPort.update(updatedNews);

            eventPort.publishUpdated(savedNews);

            if (replacementImage != null && previousImage != null) {
                imageStoragePort.delete(previousImage);
            }

            return savedNews;
        } catch (RuntimeException exception) {
            deleteReplacementAfterFailure(
                    replacementImage,
                    exception
            );

            throw exception;
        }
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

    private NewsImage storeReplacementIfPresent(
            UpdateNewsCommand command
    ) {
        NewsImageUpload image = command.image();

        if (image == null) {
            return null;
        }

        return imageStoragePort.store(
                command.propertyId(),
                command.newsId(),
                image.originalFilename(),
                image.contentType(),
                image.content()
        );
    }

    private void deleteReplacementAfterFailure(
            NewsImage replacementImage,
            RuntimeException originalException
    ) {
        if (replacementImage == null) {
            return;
        }

        try {
            imageStoragePort.delete(replacementImage);
        } catch (RuntimeException cleanupException) {
            originalException.addSuppressed(cleanupException);
        }
    }
}
