package com.mycomplex.news.application.model;

import java.util.List;
import java.util.Objects;

import com.mycomplex.news.domain.model.News;

public record NewsPage(
        List<News> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public NewsPage {
        items = List.copyOf(
                Objects.requireNonNull(items)
        );

        if (page < 0) {
            throw new IllegalArgumentException(
                    "page must not be negative"
            );
        }

        if (size < 1) {
            throw new IllegalArgumentException(
                    "size must be greater than zero"
            );
        }

        if (totalElements < 0 || totalPages < 0) {
            throw new IllegalArgumentException(
                    "pagination totals must not be negative"
            );
        }
    }
}
