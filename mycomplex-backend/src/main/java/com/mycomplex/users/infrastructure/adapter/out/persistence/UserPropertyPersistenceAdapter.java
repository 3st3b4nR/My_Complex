package com.mycomplex.users.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.users.application.port.out.LoadUserPropertiesPort;
import com.mycomplex.users.domain.model.AssignedProperty;
import com.mycomplex.users.infrastructure.adapter.out.persistence.mapper.AssignedPropertyPersistenceMapper;
import com.mycomplex.users.infrastructure.adapter.out.persistence.repository.UserPropertyJpaRepository;

@Component
public class UserPropertyPersistenceAdapter
        implements LoadUserPropertiesPort {

    private final UserPropertyJpaRepository repository;
    private final AssignedPropertyPersistenceMapper mapper;

    public UserPropertyPersistenceAdapter(
            UserPropertyJpaRepository repository,
            AssignedPropertyPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignedProperty> loadActiveByAuth0Subject(
            String auth0Subject
    ) {
        return repository
                .findActiveByAuth0Subject(auth0Subject)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}