package com.mycomplex.news.infrastructure.adapter.in.rest.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.mycomplex.news.domain.model.News;
import com.mycomplex.news.infrastructure.adapter.in.rest.dto.NewsResponse;

@Mapper(
        componentModel =
                MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface NewsRestMapper {

    @Mapping(target = "newsId", source = "id")
    @Mapping(
            target = "imageAvailable",
            expression = "java(news.image() != null)"
    )
    NewsResponse toResponse(News news);

    List<NewsResponse> toResponses(List<News> news);
}
