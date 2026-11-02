package com.mycomplex.news.application.exception;

import java.util.UUID;

public final class NewsViewingForbiddenException
        extends RuntimeException {

    public NewsViewingForbiddenException(UUID propertyId) {
        super(
                "User is not an active member of property "
                        + propertyId
        );
    }
}
