package com.mycomplex.users.infrastructure.adapter.in.rest.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import com.mycomplex.users.domain.model.AssignedProperty;
import com.mycomplex.users.domain.model.PropertyRole;
import com.mycomplex.users.infrastructure.adapter.in.rest.dto.AssignedPropertyResponse;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface AssignedPropertyRestMapper {

    @Mapping(
            target = "role",
            source = "role",
            qualifiedByName = "mapRole"
    )
    AssignedPropertyResponse toResponse(
            AssignedProperty assignedProperty
    );

    List<AssignedPropertyResponse> toResponseList(
            List<AssignedProperty> assignedProperties
    );

    @Named("mapRole")
    default String mapRole(PropertyRole role) {
        return role.name();
    }
}