package com.mycomplex.meetings.infrastructure.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.mycomplex.meetings.domain.model.Meeting;
import com.mycomplex.meetings.infrastructure.adapter.out.persistence.entity.MeetingEntity;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface MeetingPersistenceMapper {

    @Mapping(
            target = "meetingUrl",
            expression = "java(meeting.meetingUrl().toString())"
    )
    MeetingEntity toEntity(Meeting meeting);

    @Mapping(target = "markReminderSent", ignore = true)
    @Mapping(
            target = "meetingUrl",
            expression = "java(java.net.URI.create(entity.getMeetingUrl()))"
    )
    Meeting toDomain(MeetingEntity entity);
}
