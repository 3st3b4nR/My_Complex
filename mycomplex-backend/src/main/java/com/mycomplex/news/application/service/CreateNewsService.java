package com.mycomplex.news.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.mycomplex.news.application.exception.NewsPublishingForbiddenException;
import com.mycomplex.news.application.port.in.CreateNewsCommand;
import com.mycomplex.news.application.port.in.CreateNewsUseCase;
import com.mycomplex.news.application.port.in.NewsImageUpload;
import com.mycomplex.news.application.port.out.LoadAuthorizedNewsPublisherPort;
import com.mycomplex.news.application.port.out.NewsImageStoragePort;
import com.mycomplex.news.application.port.out.PublishNewsEventPort;
import com.mycomplex.news.application.port.out.SaveNewsPort;
import com.mycomplex.news.domain.model.News;
import com.mycomplex.news.domain.model.NewsImage;

public final class CreateNewsService
        implements CreateNewsUseCase {

    private final LoadAuthorizedNewsPublisherPort publisherPort;
    private final SaveNewsPort saveNewsPort;
    private final NewsImageStoragePort imageStoragePort;
    private final PublishNewsEventPort eventPort;
    private final Clock clock;

    public CreateNewsService(
            LoadAuthorizedNewsPublisherPort publisherPort,
            SaveNewsPort saveNewsPort,
            NewsImageStoragePort imageStoragePort,
            PublishNewsEventPort eventPort,
            Clock clock
    ) {
        this.publisherPort = Objects.requireNonNull(publisherPort);
        this.saveNewsPort = Objects.requireNonNull(saveNewsPort);
        this.imageStoragePort = Objects.requireNonNull(
                imageStoragePort
        );
        this.eventPort = Objects.requireNonNull(eventPort);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public News create(CreateNewsCommand command) {
        Objects.requireNonNull(
                command,
                "command must not be null"
        );

        UUID authorId = publisherPort
                .findActiveAdministratorId(
                        command.propertyId(),
                        command.auth0Subject()
                )
                .orElseThrow(() ->
                        new NewsPublishingForbiddenException(
                                command.propertyId()
                        )
                );

        UUID newsId = UUID.randomUUID();
        Instant publicationTime = clock.instant();
        NewsImage storedImage = null;

        try {
            storedImage = storeImageIfPresent(
                    newsId,
                    command
            );

            News news = News.publish(
                    newsId,
                    command.propertyId(),
                    authorId,
                    command.title(),
                    command.content(),
                    storedImage,
                    publicationTime
            );

            News savedNews = saveNewsPort.save(news);

            eventPort.publishCreated(savedNews);

            return savedNews;
        } catch (RuntimeException exception) {
            deleteStoredImageAfterFailure(
                    storedImage,
                    exception
            );

            throw exception;
        }
    }

    private NewsImage storeImageIfPresent(
            UUID newsId,
            CreateNewsCommand command
    ) {
        NewsImageUpload image = command.image();

        if (image == null) {
            return null;
        }

        return imageStoragePort.store(
                command.propertyId(),
                newsId,
                image.originalFilename(),
                image.contentType(),
                image.content()
        );
    }

    private void deleteStoredImageAfterFailure(
            NewsImage storedImage,
            RuntimeException originalException
    ) {
        if (storedImage == null) {
            return;
        }

        try {
            imageStoragePort.delete(storedImage);
        } catch (RuntimeException cleanupException) {
            originalException.addSuppressed(
                    cleanupException
            );
        }
    }
}