package com.mycomplex.news.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.mycomplex.news.domain.model.News;
import com.mycomplex.news.infrastructure.adapter.out.persistence.entity.NewsEntity;
import com.mycomplex.news.infrastructure.adapter.out.persistence.mapper.NewsPersistenceMapper;
import com.mycomplex.news.infrastructure.adapter.out.persistence.repository.NewsJpaRepository;

class NewsPersistenceAdapterTest {
    private NewsJpaRepository repository;
    private NewsPersistenceMapper mapper;
    private NewsPersistenceAdapter adapter;
    private UUID propertyId;
    private UUID newsId;
    private News news;
    private NewsEntity entity;

    @BeforeEach
    void setUp() {
        repository = mock(NewsJpaRepository.class);
        mapper = mock(NewsPersistenceMapper.class);
        adapter = new NewsPersistenceAdapter(repository, mapper);
        propertyId = UUID.randomUUID();
        newsId = UUID.randomUUID();
        Instant now = Instant.now();
        news = News.publish(
                newsId, propertyId, UUID.randomUUID(), "Título", "Contenido",
                null, now);
        entity = mock(NewsEntity.class);
        when(mapper.toEntity(news)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(news);
    }

    @Test
    void savesAndMapsTheFlushedEntity() {
        when(repository.saveAndFlush(entity)).thenReturn(entity);
        assertThat(adapter.save(news)).isEqualTo(news);
        verify(repository).saveAndFlush(entity);
    }

    @Test
    void loadsAPagedPropertyResultWithDescendingPublicationOrder() {
        when(repository.findByPropertyIdAndDeletedAtIsNull(
                org.mockito.ArgumentMatchers.eq(propertyId), any()))
                .thenReturn(new PageImpl<>(
                        List.of(entity), PageRequest.of(0, 5), 8));

        var page = adapter.loadActiveByPropertyId(propertyId, 0, 5);

        assertThat(page.items()).containsExactly(news);
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(5);
        assertThat(page.totalElements()).isEqualTo(8);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    @Test
    void loadsAnActiveNewsByTenantAndUpdatesIt() {
        when(repository.findByIdAndPropertyIdAndDeletedAtIsNull(newsId, propertyId))
                .thenReturn(Optional.of(entity));
        assertThat(adapter.loadActiveByIdAndPropertyId(newsId, propertyId))
                .contains(news);
        assertThat(adapter.update(news)).isEqualTo(news);
        verify(entity).applyChanges(news);
        verify(repository).flush();
    }

    @Test
    void reportsWhenNewsDisappearsDuringUpdate() {
        when(repository.findByIdAndPropertyIdAndDeletedAtIsNull(newsId, propertyId))
                .thenReturn(Optional.empty());
        assertThat(adapter.loadActiveByIdAndPropertyId(newsId, propertyId))
                .isEmpty();
        assertThatThrownBy(() -> adapter.update(news))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disappeared");
    }
}
