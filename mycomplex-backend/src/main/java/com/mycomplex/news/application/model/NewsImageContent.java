package com.mycomplex.news.application.model;

import java.util.Arrays;

public record NewsImageContent(
        byte[] content,
        String contentType
) {

    public NewsImageContent {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException(
                    "image content must not be empty"
            );
        }

        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException(
                    "image content type must not be blank"
            );
        }

        content = Arrays.copyOf(content, content.length);
        contentType = contentType.trim();
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }
}
