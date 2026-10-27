package com.mycomplex.users.infrastructure.adapter.out.persistence;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mycomplex.users.application.port.out.LoadPropertyMembersPort;
import com.mycomplex.users.domain.model.PropertyMember;
import com.mycomplex.users.infrastructure.adapter.out.persistence.dbo.PropertyMemberDbo;
import com.mycomplex.users.infrastructure.adapter.out.persistence.dbo.PropertyMemberRow;
import com.mycomplex.users.infrastructure.adapter.out.persistence.dbo.PropertyMemberUnitDbo;
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
        Map<UUID, List<PropertyMemberRow>> rowsByUser = repository
                .findMemberRowsByPropertyId(propertyId, null)
                .stream()
                .collect(Collectors.groupingBy(
                        PropertyMemberRow::getUserId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return toMembers(rowsByUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PropertyMember> loadByMembershipId(
            UUID propertyId,
            UUID membershipId
    ) {
        Map<UUID, List<PropertyMemberRow>> rowsByUser = repository
                .findMemberRowsByPropertyId(propertyId, membershipId)
                .stream()
                .collect(Collectors.groupingBy(
                        PropertyMemberRow::getUserId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return toMembers(rowsByUser).stream().findFirst();
    }

    private List<PropertyMember> toMembers(
            Map<UUID, List<PropertyMemberRow>> rowsByUser
    ) {
        return rowsByUser.values()
                .stream()
                .map(this::toDbo)
                .map(mapper::toDomain)
                .toList();
    }

    private PropertyMemberDbo toDbo(
            List<PropertyMemberRow> memberRows
    ) {
        PropertyMemberRow member = memberRows.getFirst();

        List<PropertyMemberUnitDbo> units = memberRows.stream()
                .filter(row -> row.getUnitId() != null)
                .map(this::toUnitDbo)
                .distinct()
                .toList();

        return new PropertyMemberDbo(
                member.getMembershipId(),
                member.getUserId(),
                member.getFirstNames(),
                member.getLastNames(),
                member.getEmail(),
                member.getRole(),
                member.getUserStatus(),
                member.getRegistrationStatus(),
                member.getMembershipStatus(),
                member.getMembershipVersion(),
                units
        );
    }

    private PropertyMemberUnitDbo toUnitDbo(
            PropertyMemberRow row
    ) {
        return new PropertyMemberUnitDbo(
                row.getAssignmentId(),
                row.getUnitId(),
                row.getStructureName(),
                row.getStructureType(),
                row.getUnitNumber(),
                row.getUnitType(),
                row.getRelationshipType(),
                Boolean.TRUE.equals(row.getPrimaryUnit()),
                row.getResidentialStatus()
        );
    }
}
