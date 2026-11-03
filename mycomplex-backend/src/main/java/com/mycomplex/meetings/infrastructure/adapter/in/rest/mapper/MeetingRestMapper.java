package com.mycomplex.meetings.infrastructure.adapter.in.rest.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.mycomplex.meetings.domain.model.Meeting;
import com.mycomplex.meetings.infrastructure.adapter.in.rest.dto.MeetingResponse;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface MeetingRestMapper {

    @Mapping(target = "meetingId", source = "id")
    MeetingResponse toResponse(Meeting meeting);

    List<MeetingResponse> toResponses(List<Meeting> meetings);
}
