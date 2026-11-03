package com.mycomplex.notifications.infrastructure.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.notifications.application.port.out.StorePushInstallationPort;
import com.mycomplex.notifications.infrastructure.adapter.out.persistence.repository.PushInstallationJpaRepository;

@Component
public class PushInstallationPersistenceAdapter
        implements StorePushInstallationPort {

    private final PushInstallationJpaRepository repository;

    public PushInstallationPersistenceAdapter(
            PushInstallationJpaRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public boolean register(
            String auth0Subject,
            String installationId
    ) {
        return repository.register(auth0Subject, installationId) > 0;
    }

    @Override
    @Transactional
    public void revoke(
            String auth0Subject,
            String installationId
    ) {
        repository.revoke(auth0Subject, installationId);
    }

    @Transactional(readOnly = true)
    public List<String> loadActiveInstallationIds(
            Collection<UUID> userIds
    ) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return repository.findActiveInstallationIds(userIds);
    }
}
