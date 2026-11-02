package com.mycomplex.news.application.port.in;

import java.util.Objects;

public record NewsImageUpload(
        String originalFilename,
        String contentType,
        byte[] content
) {

    public NewsImageUpload {
        originalFilename = requireText(
                originalFilename,
                "originalFilename"
        );

        contentType = requireText(
                contentType,
                "contentType"
        );

        Objects.requireNonNull(
                content,
                "content must not be null"
        );

        if (content.length == 0) {
            throw new IllegalArgumentException(
                    "content must not be empty"
            );
        }

        content = content.clone();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }

    private static String requireText(
            String value,
            String field
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " must not be blank"
            );
        }

        return value.trim();
    }
}