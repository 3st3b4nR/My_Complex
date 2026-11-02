package com.mycomplex.news.application.exception;

import java.util.UUID;

public final class NewsNotFoundException
        extends RuntimeException {

    public NewsNotFoundException(UUID newsId) {
        super("News not found: " + newsId);
    }
}
