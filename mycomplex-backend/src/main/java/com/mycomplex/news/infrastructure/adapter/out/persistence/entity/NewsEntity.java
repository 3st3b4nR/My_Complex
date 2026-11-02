package com.mycomplex.news.infrastructure.adapter.out.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Persistable;

import com.mycomplex.news.domain.model.News;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

@Entity
@Table(name = "noticias")
public class NewsEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "copropiedad_id", nullable = false)
    private UUID propertyId;

    @Column(name = "autor_id", nullable = false)
    private UUID authorId;

    @Column(name = "actualizado_por_id", nullable = false)
    private UUID updatedById;

    @Column(name = "titulo", nullable = false, length = 180)
    private String title;

    @Column(name = "contenido", nullable = false)
    private String content;

    @Column(name = "imagen_blob_path", length = 512)
    private String imageBlobPath;

    @Column(name = "imagen_content_type", length = 100)
    private String imageContentType;

    @Column(name = "publicada_at", nullable = false)
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Transient
    private boolean newEntity = true;

    protected NewsEntity() {
    }

    public NewsEntity(
            UUID id,
            UUID propertyId,
            UUID authorId,
            UUID updatedById,
            String title,
            String content,
            String imageBlobPath,
            String imageContentType,
            Instant publishedAt,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt,
            long version
    ) {
        this.id = id;
        this.propertyId = propertyId;
        this.authorId = authorId;
        this.updatedById = updatedById;
        this.title = title;
        this.content = content;
        this.imageBlobPath = imageBlobPath;
        this.imageContentType = imageContentType;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.version = version;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markAsPersisted() {
        newEntity = false;
    }

    public UUID getPropertyId() {
        return propertyId;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public UUID getUpdatedById() {
        return updatedById;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getImageBlobPath() {
        return imageBlobPath;
    }

    public String getImageContentType() {
        return imageContentType;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public long getVersion() {
        return version;
    }

    public void applyChanges(News news) {
        if (!id.equals(news.id())) {
            throw new IllegalArgumentException(
                    "news id cannot be changed"
            );
        }

        propertyId = news.propertyId();
        authorId = news.authorId();
        updatedById = news.updatedById();
        title = news.title();
        content = news.content();
        imageBlobPath = news.image() == null
                ? null
                : news.image().blobPath();
        imageContentType = news.image() == null
                ? null
                : news.image().contentType();
        publishedAt = news.publishedAt();
        createdAt = news.createdAt();
        updatedAt = news.updatedAt();
        deletedAt = news.deletedAt();
    }
}
