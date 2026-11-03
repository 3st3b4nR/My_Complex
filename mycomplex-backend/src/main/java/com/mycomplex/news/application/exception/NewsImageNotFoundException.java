package com.mycomplex.news.application.exception;

import java.util.UUID;

public final class NewsImageNotFoundException
        extends RuntimeException {

    public NewsImageNotFoundException(UUID newsId) {
        super("Image not found for news: " + newsId);
    }
}
