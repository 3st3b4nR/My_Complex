package com.mycomplex.news.domain.model;

public record NewsImage(
        String blobPath,
        String contentType
) {

    public NewsImage {
        blobPath = requireText(blobPath, "blobPath");
        contentType = requireText(contentType, "contentType");

        if (blobPath.length() > 512) {
            throw new IllegalArgumentException(
                    "blobPath must not exceed 512 characters"
            );
        }

        if (contentType.length() > 100) {
            throw new IllegalArgumentException(
                    "contentType must not exceed 100 characters"
            );
        }
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