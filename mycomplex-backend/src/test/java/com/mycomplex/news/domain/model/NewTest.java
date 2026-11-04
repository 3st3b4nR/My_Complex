package com.mycomplex.news.domain.model;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class NewsTest {

    @Test
    void publishesNewsWithoutImage() {
        Instant now = Instant.parse(
                "2026-10-26T15:00:00Z"
        );

        News news = News.publish(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Reunión general",
                "La reunión será el próximo sábado.",
                null,
                now
        );

        assertEquals("Reunión general", news.title());
        assertNull(news.image());
        assertEquals(now, news.publishedAt());
    }

    @Test
    void updatesAndSoftDeletesActiveNews() {
        Instant now = Instant.parse("2026-10-26T15:00:00Z");
        UUID author = UUID.randomUUID();
        UUID editor = UUID.randomUUID();
        News original = News.publish(
                UUID.randomUUID(), UUID.randomUUID(), author,
                " Título ", " Contenido ", null, now);
        NewsImage image = new NewsImage(" property/image.png ", " image/png ");

        News updated = original.update(
                " Actualizada ", " Nueva ", image, editor, now.plusSeconds(60));
        assertThat(updated.title()).isEqualTo("Actualizada");
        assertThat(updated.content()).isEqualTo("Nueva");
        assertThat(updated.image().blobPath()).isEqualTo("property/image.png");
        assertThat(updated.updatedById()).isEqualTo(editor);
        assertThat(updated.isDeleted()).isFalse();

        News deleted = updated.delete(editor, now.plusSeconds(120));
        assertThat(deleted.isDeleted()).isTrue();
        assertThatThrownBy(() -> deleted.update(
                "Otro", "Contenido", null, editor, now.plusSeconds(180)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> deleted.delete(editor, now.plusSeconds(180)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validatesNewsAndImageInvariants() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> News.publish(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                " ", "Contenido", null, now))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> News.publish(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "x".repeat(181), "Contenido", null, now))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NewsImage(" ", "image/png"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NewsImage("x".repeat(513), "image/png"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NewsImage("image.png", "x".repeat(101)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
