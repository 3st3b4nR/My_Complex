package com.mycomplex.news.application.service;

import java.util.Objects;

import com.mycomplex.news.application.exception.NewsImageNotFoundException;
import com.mycomplex.news.application.exception.NewsNotFoundException;
import com.mycomplex.news.application.exception.NewsViewingForbiddenException;
import com.mycomplex.news.application.model.NewsImageContent;
import com.mycomplex.news.application.port.in.LoadNewsImageQuery;
import com.mycomplex.news.application.port.in.LoadNewsImageUseCase;
import com.mycomplex.news.application.port.out.LoadAuthorizedNewsViewerPort;
import com.mycomplex.news.application.port.out.LoadNewsImageContentPort;
import com.mycomplex.news.application.port.out.LoadNewsPort;
import com.mycomplex.news.domain.model.News;

public final class LoadNewsImageService
        implements LoadNewsImageUseCase {

    private final LoadAuthorizedNewsViewerPort viewerPort;
    private final LoadNewsPort loadNewsPort;
    private final LoadNewsImageContentPort imageContentPort;

    public LoadNewsImageService(
            LoadAuthorizedNewsViewerPort viewerPort,
            LoadNewsPort loadNewsPort,
            LoadNewsImageContentPort imageContentPort
    ) {
        this.viewerPort = Objects.requireNonNull(viewerPort);
        this.loadNewsPort = Objects.requireNonNull(loadNewsPort);
        this.imageContentPort = Objects.requireNonNull(
                imageContentPort
        );
    }

    @Override
    public NewsImageContent load(LoadNewsImageQuery query) {
        Objects.requireNonNull(
                query,
                "query must not be null"
        );

        viewerPort.findActiveMemberId(
                query.propertyId(),
                query.auth0Subject()
        ).orElseThrow(() ->
                new NewsViewingForbiddenException(
                        query.propertyId()
                )
        );

        News news = loadNewsPort
                .loadActiveByIdAndPropertyId(
                        query.newsId(),
                        query.propertyId()
                )
                .orElseThrow(() ->
                        new NewsNotFoundException(query.newsId())
                );

        if (news.image() == null) {
            throw new NewsImageNotFoundException(news.id());
        }

        return imageContentPort.load(news.image())
                .orElseThrow(() ->
                        new NewsImageNotFoundException(news.id())
                );
    }
}
