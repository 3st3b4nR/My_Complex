package com.mycomplex.users.infrastructure.adapter.in.rest.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.mycomplex.users.domain.model.PropertyMember;
import com.mycomplex.users.domain.model.PropertyMemberUnit;
import com.mycomplex.users.infrastructure.adapter.in.rest.dto.PropertyMemberResponse;
import com.mycomplex.users.infrastructure.adapter.in.rest.dto.PropertyMemberUnitResponse;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface PropertyMemberRestMapper {

    PropertyMemberResponse toResponse(PropertyMember propertyMember);

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
}
