package com.mycomplex.users.infrastructure.adapter.in.rest.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.mycomplex.properties.domain.model.AvailableResidentialUnit;
import com.mycomplex.users.domain.model.PropertyMemberDetails;
import com.mycomplex.users.domain.model.PropertyMemberUpdate;
import com.mycomplex.users.domain.model.ResidentialAssignmentUpdate;
import com.mycomplex.users.domain.model.PropertyMember;
import com.mycomplex.users.domain.model.PropertyMemberCandidate;
import com.mycomplex.users.domain.model.PropertyMemberRegistration;
import com.mycomplex.users.domain.model.PropertyMemberUnit;
import com.mycomplex.users.infrastructure.adapter.in.rest.dto.AvailableResidentialUnitResponse;
import com.mycomplex.users.infrastructure.adapter.in.rest.dto.PropertyMemberDetailsResponse;
import com.mycomplex.users.infrastructure.adapter.in.rest.dto.PropertyMemberCandidateResponse;
import com.mycomplex.users.infrastructure.adapter.in.rest.dto.PropertyMemberResponse;
import com.mycomplex.users.infrastructure.adapter.in.rest.dto.PropertyMemberUnitResponse;
import com.mycomplex.users.infrastructure.adapter.in.rest.dto.UpdatePropertyMemberRequest;
import com.mycomplex.users.infrastructure.adapter.in.rest.dto.UpdatePropertyMemberUnitRequest;
import com.mycomplex.users.infrastructure.adapter.in.rest.dto.RegisterPropertyMemberRequest;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface PropertyMemberRestMapper {

    PropertyMemberResponse toResponse(PropertyMember propertyMember);

    PropertyMemberCandidateResponse toResponse(
            PropertyMemberCandidate candidate
    );

    @Mapping(target = "unitId", source = "unit.id")
    @Mapping(target = "structureName", source = "unit.structure.name")
    @Mapping(target = "structureType", source = "unit.structure.type")
    @Mapping(target = "unitNumber", source = "unit.number")
    @Mapping(target = "unitType", source = "unit.type")
    PropertyMemberUnitResponse toResponse(
            PropertyMemberUnit propertyMemberUnit
    );

    List<PropertyMemberResponse> toResponseList(
            List<PropertyMember> propertyMembers
    );

    @Mapping(target = "membershipId", source = "member.membershipId")
    @Mapping(target = "userId", source = "member.userId")
    @Mapping(target = "firstNames", source = "member.firstNames")
    @Mapping(target = "lastNames", source = "member.lastNames")
    @Mapping(target = "email", source = "member.email")
    @Mapping(target = "role", source = "member.role")
    @Mapping(target = "userStatus", source = "member.userStatus")
    @Mapping(
            target = "registrationStatus",
            source = "member.registrationStatus"
    )
    @Mapping(
            target = "membershipStatus",
            source = "member.membershipStatus"
    )
    @Mapping(target = "version", source = "member.version")
    @Mapping(target = "units", source = "member.units")
    PropertyMemberDetailsResponse toResponse(
            PropertyMemberDetails details
    );

    AvailableResidentialUnitResponse toResponse(
            AvailableResidentialUnit unit
    );

    default PropertyMemberUpdate toDomain(
            java.util.UUID propertyId,
            java.util.UUID membershipId,
            UpdatePropertyMemberRequest request
    ) {
        return new PropertyMemberUpdate(
                propertyId,
                membershipId,
                request.role(),
                request.membershipStatus(),
                request.version(),
                request.units().stream()
                        .map(this::toDomain)
                        .toList()
        );
    }

    default ResidentialAssignmentUpdate toDomain(
            UpdatePropertyMemberUnitRequest request
    ) {
        return new ResidentialAssignmentUpdate(
                request.unitId(),
                request.relationshipType(),
                request.primary()
        );
    }

    default PropertyMemberRegistration toDomain(
            java.util.UUID propertyId,
            RegisterPropertyMemberRequest request
    ) {
        return new PropertyMemberRegistration(
                propertyId,
                request.mode(),
                request.userId(),
                request.membershipId(),
                request.firstNames(),
                request.lastNames(),
                request.email(),
                request.documentType(),
                request.documentNumber(),
                request.role(),
                request.membershipStatus(),
                request.units().stream().map(this::toDomain).toList()
        );
    }
}
