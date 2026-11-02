package com.mycomplex.news.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.news.application.model.NewsPage;
import com.mycomplex.news.application.port.out.LoadNewsPort;
import com.mycomplex.news.application.port.out.SaveNewsPort;
import com.mycomplex.news.application.port.out.UpdateNewsPort;
import com.mycomplex.news.domain.model.News;
import com.mycomplex.news.infrastructure.adapter.out.persistence.entity.NewsEntity;
import com.mycomplex.news.infrastructure.adapter.out.persistence.mapper.NewsPersistenceMapper;
import com.mycomplex.news.infrastructure.adapter.out.persistence.repository.NewsJpaRepository;

@Component
public class NewsPersistenceAdapter
        implements SaveNewsPort, LoadNewsPort, UpdateNewsPort {

    private final NewsJpaRepository repository;
    private final NewsPersistenceMapper mapper;

    public NewsPersistenceAdapter(
            NewsJpaRepository repository,
            NewsPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public News save(News news) {
        NewsEntity entity = mapper.toEntity(news);
        NewsEntity savedEntity = repository.saveAndFlush(entity);

        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public NewsPage loadActiveByPropertyId(
            UUID propertyId,
            int page,
            int size
    ) {
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "publishedAt"
                )
        );

        Page<NewsEntity> entities = repository
                .findByPropertyIdAndDeletedAtIsNull(
                        propertyId,
                        pageable
                );

        return new NewsPage(
                entities.getContent()
                        .stream()
                        .map(mapper::toDomain)
                        .toList(),
                entities.getNumber(),
                entities.getSize(),
                entities.getTotalElements(),
                entities.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<News> loadActiveByIdAndPropertyId(
            UUID newsId,
            UUID propertyId
    ) {
        return repository
                .findByIdAndPropertyIdAndDeletedAtIsNull(
                        newsId,
                        propertyId
                )
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public News update(News news) {
        NewsEntity entity = repository
                .findByIdAndPropertyIdAndDeletedAtIsNull(
                        news.id(),
                        news.propertyId()
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "News disappeared during update"
                        )
                );

        entity.applyChanges(news);
        repository.flush();

        return mapper.toDomain(entity);
    }
}
