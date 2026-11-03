package com.mycomplex.news.infrastructure.adapter.out.blob;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.mycomplex.news.application.model.NewsImageContent;
import com.mycomplex.news.application.port.out.LoadNewsImageContentPort;
import com.mycomplex.news.application.port.out.NewsImageStoragePort;
import com.mycomplex.news.domain.model.NewsImage;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

@Component
public class AzureBlobNewsImageStorageAdapter
        implements NewsImageStoragePort, LoadNewsImageContentPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            AzureBlobNewsImageStorageAdapter.class
    );
    private static final long MAXIMUM_IMAGE_SIZE =
            5L * 1024L * 1024L;

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of(
                    "image/jpeg",
                    "image/png",
                    "image/webp"
            );

    private static final Map<String, String> EXTENSIONS =
            Map.of(
                    "image/jpeg", ".jpg",
                    "image/png", ".png",
                    "image/webp", ".webp"
            );

    private final BlobContainerClient containerClient;
    private final ObservationRegistry observations;

    public AzureBlobNewsImageStorageAdapter(
            BlobContainerClient containerClient,
            ObservationRegistry observations
    ) {
        this.containerClient = containerClient;
        this.observations = observations;
    }

    @Override
    public NewsImage store(
            UUID propertyId,
            UUID newsId,
            String originalFilename,
            String contentType,
            byte[] content
    ) {
        validateImage(contentType, content);

        String normalizedContentType = contentType
                .trim()
                .toLowerCase(Locale.ROOT);

        String blobPath = buildBlobPath(
                propertyId,
                newsId,
                normalizedContentType
        );

        return observe(
                "store",
                () -> {
                    containerClient.createIfNotExists();

                    BlobClient blobClient =
                            containerClient.getBlobClient(blobPath);

                    blobClient.upload(
                            BinaryData.fromBytes(content),
                            false
                    );

                    blobClient.setHttpHeaders(
                            new BlobHttpHeaders()
                                    .setContentType(normalizedContentType)
                    );

                    return new NewsImage(
                            blobPath,
                            normalizedContentType
                    );
                },
                ignored -> "success"
        );
    }

    @Override
    public void delete(NewsImage image) {
        if (image == null) {
            return;
        }

        observe(
                "delete",
                () -> containerClient
                        .getBlobClient(image.blobPath())
                        .deleteIfExists(),
                ignored -> "success"
        );
    }

    @Override
    public Optional<NewsImageContent> load(NewsImage image) {
        return observe(
                "load",
                () -> {
                    BlobClient blobClient = containerClient
                            .getBlobClient(image.blobPath());

                    if (!blobClient.exists()) {
                        return Optional.empty();
                    }

                    return Optional.of(
                            new NewsImageContent(
                                    blobClient.downloadContent().toBytes(),
                                    image.contentType()
                            )
                    );
                },
                result -> result.isPresent()
                        ? "success"
                        : "not_found"
        );
    }

    private <T> T observe(
            String operation,
            Supplier<T> action,
            Function<T, String> outcome
    ) {
        Observation observation = Observation.start(
                "azure.blob.news.image",
                observations
        ).lowCardinalityKeyValue("operation", operation);

        try {
            T result;
            try (Observation.Scope ignored = observation.openScope()) {
                result = action.get();
            }
            observation.lowCardinalityKeyValue(
                    "outcome",
                    outcome.apply(result)
            );
            return result;
        } catch (RuntimeException exception) {
            observation.lowCardinalityKeyValue("outcome", "error");
            observation.error(exception);
            LOGGER.atWarn()
                    .addKeyValue("integration", "azure_blob")
                    .addKeyValue("operation", operation)
                    .log("News image storage operation failed");
            throw exception;
        } finally {
            observation.stop();
        }
    }

    private void validateImage(
            String contentType,
            byte[] content
    ) {
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException(
                    "Image content type is required"
            );
        }

        if (content == null || content.length == 0) {
            throw new IllegalArgumentException(
                    "Image content is required"
            );
        }

        if (content.length > MAXIMUM_IMAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Image must not exceed 5 MB"
            );
        }

        String normalizedContentType = contentType
                .trim()
                .toLowerCase(Locale.ROOT);

        if (!ALLOWED_CONTENT_TYPES.contains(
                normalizedContentType
        )) {
            throw new IllegalArgumentException(
                    "Only JPEG, PNG and WEBP images are allowed"
            );
        }

        if (!hasValidSignature(
                normalizedContentType,
                content
        )) {
            throw new IllegalArgumentException(
                    "Image content does not match its content type"
            );
        }
    }

    private boolean hasValidSignature(
            String contentType,
            byte[] content
    ) {
        return switch (contentType) {
            case "image/jpeg" ->
                    content.length >= 3
                    && unsigned(content[0]) == 0xFF
                    && unsigned(content[1]) == 0xD8
                    && unsigned(content[2]) == 0xFF;

            case "image/png" ->
                    content.length >= 8
                    && unsigned(content[0]) == 0x89
                    && unsigned(content[1]) == 0x50
                    && unsigned(content[2]) == 0x4E
                    && unsigned(content[3]) == 0x47
                    && unsigned(content[4]) == 0x0D
                    && unsigned(content[5]) == 0x0A
                    && unsigned(content[6]) == 0x1A
                    && unsigned(content[7]) == 0x0A;

            case "image/webp" ->
                    content.length >= 12
                    && content[0] == 'R'
                    && content[1] == 'I'
                    && content[2] == 'F'
                    && content[3] == 'F'
                    && content[8] == 'W'
                    && content[9] == 'E'
                    && content[10] == 'B'
                    && content[11] == 'P';

            default -> false;
        };
    }

    private String buildBlobPath(
            UUID propertyId,
            UUID newsId,
            String contentType
    ) {
        return "%s/news/%s/%s%s".formatted(
                propertyId,
                newsId,
                UUID.randomUUID(),
                EXTENSIONS.get(contentType)
        );
    }

    private int unsigned(byte value) {
        return Byte.toUnsignedInt(value);
    }
}
