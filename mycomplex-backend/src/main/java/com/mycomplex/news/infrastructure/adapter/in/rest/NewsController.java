package com.mycomplex.news.infrastructure.adapter.in.rest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mycomplex.news.application.port.in.CreateNewsCommand;
import com.mycomplex.news.application.port.in.CreateNewsUseCase;
import com.mycomplex.news.application.port.in.DeleteNewsCommand;
import com.mycomplex.news.application.port.in.DeleteNewsUseCase;
import com.mycomplex.news.application.port.in.ListNewsQuery;
import com.mycomplex.news.application.port.in.ListNewsUseCase;
import com.mycomplex.news.application.port.in.LoadNewsImageQuery;
import com.mycomplex.news.application.port.in.LoadNewsImageUseCase;
import com.mycomplex.news.application.port.in.NewsImageUpload;
import com.mycomplex.news.application.port.in.UpdateNewsCommand;
import com.mycomplex.news.application.port.in.UpdateNewsUseCase;
import com.mycomplex.news.domain.model.News;
import com.mycomplex.news.infrastructure.adapter.in.rest.dto.CreateNewsRequest;
import com.mycomplex.news.infrastructure.adapter.in.rest.dto.NewsPageResponse;
import com.mycomplex.news.infrastructure.adapter.in.rest.dto.NewsResponse;
import com.mycomplex.news.infrastructure.adapter.in.rest.dto.UpdateNewsRequest;
import com.mycomplex.news.infrastructure.adapter.in.rest.mapper.NewsRestMapper;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/properties/{propertyId}/news")
@Validated
public class NewsController {

    private final CreateNewsUseCase createNewsUseCase;
    private final DeleteNewsUseCase deleteNewsUseCase;
    private final ListNewsUseCase listNewsUseCase;
    private final LoadNewsImageUseCase loadNewsImageUseCase;
    private final UpdateNewsUseCase updateNewsUseCase;
    private final NewsRestMapper mapper;

    public NewsController(
            CreateNewsUseCase createNewsUseCase,
            DeleteNewsUseCase deleteNewsUseCase,
            ListNewsUseCase listNewsUseCase,
            LoadNewsImageUseCase loadNewsImageUseCase,
            UpdateNewsUseCase updateNewsUseCase,
            NewsRestMapper mapper
    ) {
        this.createNewsUseCase = createNewsUseCase;
        this.deleteNewsUseCase = deleteNewsUseCase;
        this.listNewsUseCase = listNewsUseCase;
        this.loadNewsImageUseCase = loadNewsImageUseCase;
        this.updateNewsUseCase = updateNewsUseCase;
        this.mapper = mapper;
    }

    @GetMapping("/{newsId}/image")
    public ResponseEntity<byte[]> loadImage(
            @PathVariable UUID propertyId,
            @PathVariable UUID newsId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var image = loadNewsImageUseCase.load(
                new LoadNewsImageQuery(
                        propertyId,
                        newsId,
                        jwt.getSubject()
                )
        );

        byte[] content = image.content();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(
                        MediaType.parseMediaType(
                                image.contentType()
                        )
                )
                .contentLength(content.length)
                .body(content);
    }

    @DeleteMapping("/{newsId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID propertyId,
            @PathVariable UUID newsId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam
            @Min(0)
            long version
    ) {
        DeleteNewsCommand command = new DeleteNewsCommand(
                propertyId,
                newsId,
                jwt.getSubject(),
                version
        );

        deleteNewsUseCase.delete(command);
    }

    @GetMapping
    public NewsPageResponse list(
            @PathVariable UUID propertyId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,
            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            int size
    ) {
        ListNewsQuery query = new ListNewsQuery(
                propertyId,
                jwt.getSubject(),
                page,
                size
        );

        var result = listNewsUseCase.list(query);

        return new NewsPageResponse(
                mapper.toResponses(result.items()),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    @PutMapping(
            path = "/{newsId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public NewsResponse update(
            @PathVariable UUID propertyId,
            @PathVariable UUID newsId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid
            @RequestPart("data")
            UpdateNewsRequest request,
            @RequestPart(
                    value = "image",
                    required = false
            )
            MultipartFile image
    ) {
        UpdateNewsCommand command = new UpdateNewsCommand(
                propertyId,
                newsId,
                jwt.getSubject(),
                request.title(),
                request.content(),
                request.version(),
                toImageUpload(image)
        );

        return mapper.toResponse(
                updateNewsUseCase.update(command)
        );
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public NewsResponse create(
            @PathVariable UUID propertyId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid
            @RequestPart("data")
            CreateNewsRequest request,
            @RequestPart(
                    value = "image",
                    required = false
            )
            MultipartFile image
    ) {
        CreateNewsCommand command = new CreateNewsCommand(
                propertyId,
                jwt.getSubject(),
                request.title(),
                request.content(),
                toImageUpload(image)
        );

        News news = createNewsUseCase.create(command);

        return mapper.toResponse(news);
    }

    private NewsImageUpload toImageUpload(
            MultipartFile image
    ) {
        if (image == null || image.isEmpty()) {
            return null;
        }

        try {
            return new NewsImageUpload(
                    image.getOriginalFilename(),
                    image.getContentType(),
                    image.getBytes()
            );
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Could not read uploaded image",
                    exception
            );
        }
    }
}
