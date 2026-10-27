package com.mycomplex.users.infrastructure.adapter.in.rest.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.mycomplex.users.domain.model.PropertyMember;
import com.mycomplex.users.infrastructure.adapter.in.rest.dto.PropertyMemberResponse;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface PropertyMemberRestMapper {

    PropertyMemberResponse toResponse(PropertyMember propertyMember);

    List<PropertyMemberResponse> toResponseList(
            List<PropertyMember> propertyMembers
    );
}
