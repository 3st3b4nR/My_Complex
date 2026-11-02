package com.mycomplex.news.application.exception;

import java.util.UUID;

public final class NewsPublishingForbiddenException
        extends RuntimeException {

    public NewsPublishingForbiddenException(UUID propertyId) {
        super(
                "User is not an active administrator of property "
                        + propertyId
        );
    }
}