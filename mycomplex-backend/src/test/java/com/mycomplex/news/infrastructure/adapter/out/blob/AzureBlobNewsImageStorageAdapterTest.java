package com.mycomplex.news.infrastructure.adapter.out.blob;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.mycomplex.news.application.model.NewsImageContent;
import com.mycomplex.news.domain.model.NewsImage;

import io.micrometer.observation.tck.TestObservationRegistry;

class AzureBlobNewsImageStorageAdapterTest {

    private static final byte[] PNG = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A
    };

    private BlobContainerClient containerClient;
    private BlobClient blobClient;
    private TestObservationRegistry observations;
    private AzureBlobNewsImageStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        containerClient = mock(BlobContainerClient.class);
        blobClient = mock(BlobClient.class);
        observations = TestObservationRegistry.create();
        when(containerClient.getBlobClient(anyString()))
                .thenReturn(blobClient);
        adapter = new AzureBlobNewsImageStorageAdapter(
                containerClient,
                observations
        );
    }

    @Test
    void observesSuccessfulStoreWithoutBlobIdentifiers() {
        NewsImage image = adapter.store(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "community-update.png",
                "image/png",
                PNG
        );

        assertThat(image.contentType()).isEqualTo("image/png");
        assertThat(image.blobPath()).endsWith(".png");
        assertObservation("store", "success", false);
    }

    @Test
    void observesMissingImageAsNotFound() {
        NewsImage image = new NewsImage(
                "property/news/missing.png",
                "image/png"
        );
        when(blobClient.exists()).thenReturn(false);

        Optional<NewsImageContent> result = adapter.load(image);

        assertThat(result).isEmpty();
        assertObservation("load", "not_found", false);
    }

    @Test
    void observesSuccessfulDeleteWithoutBlobIdentifiers() {
        adapter.delete(new NewsImage(
                "property/news/obsolete.png",
                "image/png"
        ));

        assertObservation("delete", "success", false);
    }

    @Test
    void observesStorageFailureAndRethrowsIt() {
        doThrow(new IllegalStateException("storage unavailable"))
                .when(blobClient)
                .upload(any(BinaryData.class), eq(false));

        assertThatThrownBy(() -> adapter.store(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "community-update.png",
                "image/png",
                PNG
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("storage unavailable");

        assertObservation("store", "error", true);
    }

    private void assertObservation(
            String operation,
            String outcome,
            boolean hasError
    ) {
        var assertion = observations.assertThat()
                .hasSingleObservationThat()
                .hasNameEqualTo("azure.blob.news.image")
                .hasLowCardinalityKeyValue("operation", operation)
                .hasLowCardinalityKeyValue("outcome", outcome)
                .hasOnlyKeys("operation", "outcome");

        if (hasError) {
            assertion.hasError();
        } else {
            assertion.doesNotHaveError();
        }
    }
}
