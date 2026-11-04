package com.mycomplex.news.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;

import com.mycomplex.news.application.model.NewsImageContent;
import com.mycomplex.news.application.model.NewsPage;
import com.mycomplex.news.application.port.in.CreateNewsCommand;
import com.mycomplex.news.application.port.in.CreateNewsUseCase;
import com.mycomplex.news.application.port.in.DeleteNewsCommand;
import com.mycomplex.news.application.port.in.DeleteNewsUseCase;
import com.mycomplex.news.application.port.in.ListNewsUseCase;
import com.mycomplex.news.application.port.in.LoadNewsImageUseCase;
import com.mycomplex.news.application.port.in.UpdateNewsCommand;
import com.mycomplex.news.application.port.in.UpdateNewsUseCase;
import com.mycomplex.news.domain.model.News;
import com.mycomplex.news.infrastructure.adapter.in.rest.dto.CreateNewsRequest;
import com.mycomplex.news.infrastructure.adapter.in.rest.dto.NewsResponse;
import com.mycomplex.news.infrastructure.adapter.in.rest.dto.UpdateNewsRequest;
import com.mycomplex.news.infrastructure.adapter.in.rest.mapper.NewsRestMapper;

class NewsControllerDelegationTest {
    private CreateNewsUseCase create;
    private DeleteNewsUseCase delete;
    private ListNewsUseCase list;
    private LoadNewsImageUseCase loadImage;
    private UpdateNewsUseCase update;
    private NewsRestMapper mapper;
    private NewsController controller;
    private Jwt jwt;
    private UUID propertyId;
    private UUID newsId;
    private News news;
    private NewsResponse response;

    @BeforeEach
    void setUp() {
        create = mock(CreateNewsUseCase.class);
        delete = mock(DeleteNewsUseCase.class);
        list = mock(ListNewsUseCase.class);
        loadImage = mock(LoadNewsImageUseCase.class);
        update = mock(UpdateNewsUseCase.class);
        mapper = mock(NewsRestMapper.class);
        controller = new NewsController(
                create, delete, list, loadImage, update, mapper);
        jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("auth0|admin");
        propertyId = UUID.randomUUID();
        newsId = UUID.randomUUID();
        Instant now = Instant.now();
        news = News.publish(
                newsId, propertyId, UUID.randomUUID(), "Título", "Contenido",
                null, now);
        response = new NewsResponse(
                newsId, propertyId, "Título", "Contenido", false,
                now, now, 0);
        when(mapper.toResponse(news)).thenReturn(response);
    }

    @Test
    void createsAndUpdatesMultipartNews() throws IOException {
        MultipartFile image = mock(MultipartFile.class);
        when(image.isEmpty()).thenReturn(false);
        when(image.getOriginalFilename()).thenReturn("notice.png");
        when(image.getContentType()).thenReturn("image/png");
        when(image.getBytes()).thenReturn(new byte[]{1});
        when(create.create(any(CreateNewsCommand.class))).thenReturn(news);
        assertThat(controller.create(
                propertyId, jwt, new CreateNewsRequest("Título", "Contenido"),
                image)).isEqualTo(response);
        verify(create).create(any(CreateNewsCommand.class));

        when(update.update(any(UpdateNewsCommand.class))).thenReturn(news);
        assertThat(controller.update(
                propertyId, newsId, jwt,
                new UpdateNewsRequest("Título", "Contenido", 0), null))
                .isEqualTo(response);
        verify(update).update(any(UpdateNewsCommand.class));
    }

    @Test
    void listsDeletesAndLoadsImages() {
        when(list.list(any())).thenReturn(new NewsPage(
                List.of(news), 0, 20, 1, 1));
        when(mapper.toResponses(List.of(news))).thenReturn(List.of(response));
        var page = controller.list(propertyId, jwt, 0, 20);
        assertThat(page.items()).containsExactly(response);
        assertThat(page.totalElements()).isEqualTo(1);

        controller.delete(propertyId, newsId, jwt, 2);
        verify(delete).delete(new DeleteNewsCommand(
                propertyId, newsId, "auth0|admin", 2));

        when(loadImage.load(any())).thenReturn(new NewsImageContent(
                new byte[]{1, 2, 3}, "image/png"));
        var image = controller.loadImage(propertyId, newsId, jwt);
        assertThat(image.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(image.getHeaders().getContentType())
                .isEqualTo(MediaType.IMAGE_PNG);
        assertThat(image.getBody()).containsExactly(1, 2, 3);
    }

    @Test
    void rejectsUnreadableUploadedImagesAndAcceptsEmptyImages() throws IOException {
        MultipartFile broken = mock(MultipartFile.class);
        when(broken.isEmpty()).thenReturn(false);
        when(broken.getBytes()).thenThrow(new IOException("broken"));
        when(create.create(any())).thenReturn(news);
        assertThatThrownBy(() -> controller.create(
                propertyId, jwt, new CreateNewsRequest("Título", "Contenido"),
                broken)).isInstanceOf(UncheckedIOException.class);

        MultipartFile empty = mock(MultipartFile.class);
        when(empty.isEmpty()).thenReturn(true);
        assertThat(controller.create(
                propertyId, jwt, new CreateNewsRequest("Título", "Contenido"),
                empty)).isEqualTo(response);
    }
}
