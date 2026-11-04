package com.mycomplex.news.infrastructure.adapter.out.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.mycomplex.news.domain.model.News;
import com.mycomplex.news.domain.model.NewsImage;

class NewsEntityTest {
    @Test
    void exposesPersistenceStateAndAppliesDomainChanges() {
        UUID id = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID editorId = UUID.randomUUID();
        Instant created = Instant.parse("2026-11-01T10:00:00Z");
        NewsEntity entity = new NewsEntity(
                id, propertyId, authorId, authorId, "Original", "Contenido",
                null, null, created, created, created, null, 3);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.isNew()).isTrue();
        entity.markAsPersisted();
        assertThat(entity.isNew()).isFalse();

        News changed = new News(
                id, propertyId, authorId, editorId, "Actualizada", "Nuevo",
                new NewsImage("property/image.png", "image/png"), created,
                created, created.plusSeconds(60), created.plusSeconds(60), 3);
        entity.applyChanges(changed);

        assertThat(entity.getPropertyId()).isEqualTo(propertyId);
        assertThat(entity.getAuthorId()).isEqualTo(authorId);
        assertThat(entity.getUpdatedById()).isEqualTo(editorId);
        assertThat(entity.getTitle()).isEqualTo("Actualizada");
        assertThat(entity.getContent()).isEqualTo("Nuevo");
        assertThat(entity.getImageBlobPath()).isEqualTo("property/image.png");
        assertThat(entity.getImageContentType()).isEqualTo("image/png");
        assertThat(entity.getPublishedAt()).isEqualTo(created);
        assertThat(entity.getCreatedAt()).isEqualTo(created);
        assertThat(entity.getUpdatedAt()).isEqualTo(created.plusSeconds(60));
        assertThat(entity.getDeletedAt()).isEqualTo(created.plusSeconds(60));
        assertThat(entity.getVersion()).isEqualTo(3);

        entity.applyChanges(new News(
                id, propertyId, authorId, editorId, "Sin imagen", "Nuevo",
                null, created, created, created, null, 3));
        assertThat(entity.getImageBlobPath()).isNull();
        assertThat(entity.getImageContentType()).isNull();
    }

    @Test
    void rejectsChangingTheEntityIdentity() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        NewsEntity entity = new NewsEntity(
                id, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Título", "Contenido", null, null, now, now, now, null, 0);
        News other = News.publish(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Otro", "Contenido", null, now);

        assertThatThrownBy(() -> entity.applyChanges(other))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id cannot be changed");
    }
}
