package com.mycomplex.users.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.users.application.port.out.LoadLocalUserIdPort;

@Component
public class LocalUserIdPersistenceAdapter implements LoadLocalUserIdPort {

    private final JdbcTemplate jdbcTemplate;

    public LocalUserIdPersistenceAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findByAuth0Subject(String auth0Subject) {
        List<UUID> ids = jdbcTemplate.query(
                """
                SELECT id
                FROM usuarios
                WHERE auth0_subject = ?
                  AND deleted_at IS NULL
                """,
                (resultSet, rowNumber) -> resultSet.getObject("id", UUID.class),
                auth0Subject
        );
        return ids.stream().findFirst();
    }
}
