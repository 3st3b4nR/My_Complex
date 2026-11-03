package com.mycomplex.news.application.service;

import java.util.Objects;

import com.mycomplex.news.application.exception.NewsViewingForbiddenException;
import com.mycomplex.news.application.model.NewsPage;
import com.mycomplex.news.application.port.in.ListNewsQuery;
import com.mycomplex.news.application.port.in.ListNewsUseCase;
import com.mycomplex.news.application.port.out.LoadAuthorizedNewsViewerPort;
import com.mycomplex.news.application.port.out.LoadNewsPort;

public final class ListNewsService
        implements ListNewsUseCase {

    private final LoadAuthorizedNewsViewerPort viewerPort;
    private final LoadNewsPort loadNewsPort;

    public ListNewsService(
            LoadAuthorizedNewsViewerPort viewerPort,
            LoadNewsPort loadNewsPort
    ) {
        this.viewerPort = Objects.requireNonNull(viewerPort);
        this.loadNewsPort = Objects.requireNonNull(loadNewsPort);
    }

    @Override
    public NewsPage list(ListNewsQuery query) {
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

        return loadNewsPort.loadActiveByPropertyId(
                query.propertyId(),
                query.page(),
                query.size()
        );
    }
}
