package com.mycomplex.news.domain.model;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
}