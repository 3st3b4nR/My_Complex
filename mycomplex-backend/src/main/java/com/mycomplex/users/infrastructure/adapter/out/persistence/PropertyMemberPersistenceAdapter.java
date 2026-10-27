package com.mycomplex.users.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.users.application.port.out.LoadPropertyMembersPort;
import com.mycomplex.users.domain.model.PropertyMember;
import com.mycomplex.users.infrastructure.adapter.out.persistence.mapper.PropertyMemberPersistenceMapper;
import com.mycomplex.users.infrastructure.adapter.out.persistence.repository.UserPropertyJpaRepository;

@Component
public class PropertyMemberPersistenceAdapter
        implements LoadPropertyMembersPort {

    private final UserPropertyJpaRepository repository;
    private final PropertyMemberPersistenceMapper mapper;

    public PropertyMemberPersistenceAdapter(
            UserPropertyJpaRepository repository,
            PropertyMemberPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveAdministratorAccess(
            UUID propertyId,
            String auth0Subject
    ) {
        return repository.hasActiveAdministratorAccess(
                propertyId,
                auth0Subject
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertyMember> loadByPropertyId(UUID propertyId) {
        return repository.findMembersByPropertyId(propertyId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
