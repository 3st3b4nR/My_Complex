package com.mycomplex.news.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNewsRequest(

        @NotBlank
        @Size(max = 180)
        String title,

        @NotBlank
        String content
) {
}