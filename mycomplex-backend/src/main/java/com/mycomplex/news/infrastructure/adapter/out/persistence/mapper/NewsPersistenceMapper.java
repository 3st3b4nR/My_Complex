package com.mycomplex.news.infrastructure.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.mycomplex.news.domain.model.News;
import com.mycomplex.news.domain.model.NewsImage;
import com.mycomplex.news.infrastructure.adapter.out.persistence.entity.NewsEntity;

@Mapper(
        componentModel =
                MappingConstants.ComponentModel.SPRING
)
public interface NewsPersistenceMapper {

    @Mapping(
            target = "imageBlobPath",
            expression = """
                    java(news.image() == null
                        ? null
                        : news.image().blobPath())
                    """
    )
    @Mapping(
            target = "imageContentType",
            expression = """
                    java(news.image() == null
                        ? null
                        : news.image().contentType())
                    """
    )
    NewsEntity toEntity(News news);

    @Mapping(
            target = "image",
            expression = """
                    java(toImage(
                        entity.getImageBlobPath(),
                        entity.getImageContentType()
                    ))
                    """
    )
    News toDomain(NewsEntity entity);

    default NewsImage toImage(
            String blobPath,
            String contentType
    ) {
        if (blobPath == null || contentType == null) {
            return null;
        }

        return new NewsImage(blobPath, contentType);
    }
}