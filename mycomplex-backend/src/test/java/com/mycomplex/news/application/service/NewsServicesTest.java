package com.mycomplex.news.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mycomplex.news.application.exception.NewsNotFoundException;
import com.mycomplex.news.application.exception.NewsPublishingForbiddenException;
import com.mycomplex.news.application.exception.NewsVersionConflictException;
import com.mycomplex.news.application.port.in.CreateNewsCommand;
import com.mycomplex.news.application.port.in.DeleteNewsCommand;
import com.mycomplex.news.application.port.in.NewsImageUpload;
import com.mycomplex.news.application.port.in.UpdateNewsCommand;
import com.mycomplex.news.application.port.out.LoadAuthorizedNewsPublisherPort;
import com.mycomplex.news.application.port.out.LoadNewsPort;
import com.mycomplex.news.application.port.out.NewsImageStoragePort;
import com.mycomplex.news.application.port.out.PublishNewsEventPort;
import com.mycomplex.news.application.port.out.SaveNewsPort;
import com.mycomplex.news.application.port.out.ScheduleNewsImageDeletionPort;
import com.mycomplex.news.application.port.out.UpdateNewsPort;
import com.mycomplex.news.domain.model.News;
import com.mycomplex.news.domain.model.NewsImage;

class NewsServicesTest {
    private static final UUID PROPERTY_ID = UUID.randomUUID();
    private static final UUID NEWS_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-03T15:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final NewsImage OLD_IMAGE =
            new NewsImage("property/old.png", "image/png");
    private static final NewsImage NEW_IMAGE =
            new NewsImage("property/new.png", "image/png");

    private LoadAuthorizedNewsPublisherPort publisher;
    private LoadNewsPort loader;
    private SaveNewsPort saver;
    private UpdateNewsPort updater;
    private NewsImageStoragePort images;
    private PublishNewsEventPort events;
    private ScheduleNewsImageDeletionPort deletion;

    @BeforeEach
    void setUp() {
        publisher = mock(LoadAuthorizedNewsPublisherPort.class);
        loader = mock(LoadNewsPort.class);
        saver = mock(SaveNewsPort.class);
        updater = mock(UpdateNewsPort.class);
        images = mock(NewsImageStoragePort.class);
        events = mock(PublishNewsEventPort.class);
        deletion = mock(ScheduleNewsImageDeletionPort.class);
        when(publisher.findActiveAdministratorId(PROPERTY_ID, "auth0|admin"))
                .thenReturn(Optional.of(ADMIN_ID));
    }

    @Test
    void createsNewsWithoutImageAndPublishesTheSavedResult() {
        CreateNewsService service = new CreateNewsService(
                publisher, saver, images, events, CLOCK);
        when(saver.save(any(News.class))).thenAnswer(invocation ->
                invocation.getArgument(0));

        News saved = service.create(new CreateNewsCommand(
                PROPERTY_ID, "auth0|admin", " Aviso ", " Contenido ", null));

        assertThat(saved.title()).isEqualTo("Aviso");
        assertThat(saved.publishedAt()).isEqualTo(NOW);
        assertThat(saved.image()).isNull();
        verify(images, never()).store(any(), any(), any(), any(), any());
        verify(events).publishCreated(saved);
    }

    @Test
    void storesAnOptionalImageAndCleansItWhenPersistenceFails() {
        CreateNewsService service = new CreateNewsService(
                publisher, saver, images, events, CLOCK);
        NewsImageUpload upload = new NewsImageUpload(
                "notice.png", "image/png", new byte[]{1, 2});
        when(images.store(any(), any(), any(), any(), any()))
                .thenReturn(NEW_IMAGE);
        IllegalStateException persistenceFailure =
                new IllegalStateException("database unavailable");
        when(saver.save(any())).thenThrow(persistenceFailure);
        IllegalStateException cleanupFailure =
                new IllegalStateException("blob unavailable");
        doThrow(cleanupFailure).when(images).delete(NEW_IMAGE);

        assertThatThrownBy(() -> service.create(new CreateNewsCommand(
                PROPERTY_ID, "auth0|admin", "Aviso", "Contenido", upload)))
                .isSameAs(persistenceFailure)
                .satisfies(error -> assertThat(error.getSuppressed())
                        .containsExactly(cleanupFailure));
        verify(images).delete(NEW_IMAGE);
        verify(events, never()).publishCreated(any());
    }

    @Test
    void rejectsCreationWhenTheActorIsNotAnAdministrator() {
        when(publisher.findActiveAdministratorId(PROPERTY_ID, "auth0|other"))
                .thenReturn(Optional.empty());
        CreateNewsService service = new CreateNewsService(
                publisher, saver, images, events, CLOCK);

        assertThatThrownBy(() -> service.create(new CreateNewsCommand(
                PROPERTY_ID, "auth0|other", "Aviso", "Contenido", null)))
                .isInstanceOf(NewsPublishingForbiddenException.class);
        verify(saver, never()).save(any());
    }

    @Test
    void updatesNewsAndReplacesThePreviousImage() {
        News current = news(OLD_IMAGE, 3);
        when(loader.loadActiveByIdAndPropertyId(NEWS_ID, PROPERTY_ID))
                .thenReturn(Optional.of(current));
        when(images.store(any(), any(), any(), any(), any()))
                .thenReturn(NEW_IMAGE);
        when(updater.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
        UpdateNewsService service = new UpdateNewsService(
                publisher, loader, updater, images, events, CLOCK);

        News saved = service.update(new UpdateNewsCommand(
                PROPERTY_ID, NEWS_ID, "auth0|admin", "Nuevo título",
                "Nuevo contenido", 3,
                new NewsImageUpload("new.png", "image/png", new byte[]{1})));

        assertThat(saved.image()).isEqualTo(NEW_IMAGE);
        assertThat(saved.updatedById()).isEqualTo(ADMIN_ID);
        verify(images).delete(OLD_IMAGE);
        verify(events).publishUpdated(saved);
    }

    @Test
    void keepsTheExistingImageWhenNoReplacementWasProvided() {
        News current = news(OLD_IMAGE, 0);
        when(loader.loadActiveByIdAndPropertyId(NEWS_ID, PROPERTY_ID))
                .thenReturn(Optional.of(current));
        when(updater.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
        UpdateNewsService service = new UpdateNewsService(
                publisher, loader, updater, images, events, CLOCK);

        News saved = service.update(new UpdateNewsCommand(
                PROPERTY_ID, NEWS_ID, "auth0|admin", "Título", "Contenido",
                0, null));

        assertThat(saved.image()).isEqualTo(OLD_IMAGE);
        verify(images, never()).store(any(), any(), any(), any(), any());
        verify(images, never()).delete(any());
    }

    @Test
    void removesAReplacementWhenUpdatingPersistenceFails() {
        when(loader.loadActiveByIdAndPropertyId(NEWS_ID, PROPERTY_ID))
                .thenReturn(Optional.of(news(null, 0)));
        when(images.store(any(), any(), any(), any(), any()))
                .thenReturn(NEW_IMAGE);
        when(updater.update(any())).thenThrow(new IllegalStateException("db"));
        UpdateNewsService service = new UpdateNewsService(
                publisher, loader, updater, images, events, CLOCK);

        assertThatThrownBy(() -> service.update(new UpdateNewsCommand(
                PROPERTY_ID, NEWS_ID, "auth0|admin", "Título", "Contenido", 0,
                new NewsImageUpload("new.png", "image/png", new byte[]{1}))))
                .isInstanceOf(IllegalStateException.class);
        verify(images).delete(NEW_IMAGE);
    }

    @Test
    void reportsMissingAndStaleNewsDuringUpdate() {
        UpdateNewsService service = new UpdateNewsService(
                publisher, loader, updater, images, events, CLOCK);
        when(loader.loadActiveByIdAndPropertyId(NEWS_ID, PROPERTY_ID))
                .thenReturn(Optional.empty());
        UpdateNewsCommand command = new UpdateNewsCommand(
                PROPERTY_ID, NEWS_ID, "auth0|admin", "Título", "Contenido", 0,
                null);
        assertThatThrownBy(() -> service.update(command))
                .isInstanceOf(NewsNotFoundException.class);

        when(loader.loadActiveByIdAndPropertyId(NEWS_ID, PROPERTY_ID))
                .thenReturn(Optional.of(news(null, 4)));
        assertThatThrownBy(() -> service.update(command))
                .isInstanceOf(NewsVersionConflictException.class);
        verify(updater, never()).update(any());
    }

    @Test
    void softDeletesNewsAndSchedulesItsImageDeletion() {
        News current = news(OLD_IMAGE, 2);
        when(loader.loadActiveByIdAndPropertyId(NEWS_ID, PROPERTY_ID))
                .thenReturn(Optional.of(current));
        DeleteNewsService service = new DeleteNewsService(
                publisher, loader, updater, deletion, CLOCK);

        service.delete(new DeleteNewsCommand(
                PROPERTY_ID, NEWS_ID, "auth0|admin", 2));

        verify(updater).update(any(News.class));
        verify(deletion).schedule(OLD_IMAGE);
    }

    @Test
    void rejectsMissingStaleAndUnauthorizedDeletion() {
        DeleteNewsService service = new DeleteNewsService(
                publisher, loader, updater, deletion, CLOCK);
        DeleteNewsCommand command = new DeleteNewsCommand(
                PROPERTY_ID, NEWS_ID, "auth0|admin", 0);
        when(loader.loadActiveByIdAndPropertyId(NEWS_ID, PROPERTY_ID))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(command))
                .isInstanceOf(NewsNotFoundException.class);

        when(loader.loadActiveByIdAndPropertyId(NEWS_ID, PROPERTY_ID))
                .thenReturn(Optional.of(news(null, 1)));
        assertThatThrownBy(() -> service.delete(command))
                .isInstanceOf(NewsVersionConflictException.class);

        when(publisher.findActiveAdministratorId(PROPERTY_ID, "auth0|admin"))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(command))
                .isInstanceOf(NewsPublishingForbiddenException.class);
    }

    private News news(NewsImage image, long version) {
        return new News(
                NEWS_ID, PROPERTY_ID, ADMIN_ID, ADMIN_ID, "Título", "Contenido",
                image, NOW.minusSeconds(3600), NOW.minusSeconds(3600),
                NOW.minusSeconds(3600), null, version);
    }
}
