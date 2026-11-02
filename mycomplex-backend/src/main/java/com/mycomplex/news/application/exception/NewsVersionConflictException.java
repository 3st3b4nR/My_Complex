package com.mycomplex.news.application.exception;

import java.util.UUID;

public final class NewsVersionConflictException
        extends RuntimeException {

    public NewsVersionConflictException(
            UUID newsId,
            long expectedVersion,
            long receivedVersion
    ) {
        super(
                "News %s has version %d, but version %d was received"
                        .formatted(
                                newsId,
                                expectedVersion,
                                receivedVersion
                        )
        );
    }
}
