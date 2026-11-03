package com.mycomplex.news.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mycomplex.news.infrastructure.adapter.out.persistence.entity.NewsEntity;

public interface NewsJpaRepository
        extends JpaRepository<NewsEntity, UUID> {

    Page<NewsEntity> findByPropertyIdAndDeletedAtIsNull(
            UUID propertyId,
            Pageable pageable
    );

    Optional<NewsEntity>
            findByIdAndPropertyIdAndDeletedAtIsNull(
                    UUID id,
                    UUID propertyId
            );
}
