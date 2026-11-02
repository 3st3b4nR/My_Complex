package com.mycomplex.news.infrastructure.adapter.in.rest.dto;

import java.util.List;

public record NewsPageResponse(
        List<NewsResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public NewsPageResponse {
        items = List.copyOf(items);
    }
}
