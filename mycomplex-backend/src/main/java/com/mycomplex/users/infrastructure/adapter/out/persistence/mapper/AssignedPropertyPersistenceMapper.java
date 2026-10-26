package com.mycomplex.users.infrastructure.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import com.mycomplex.users.domain.model.AssignedProperty;
import com.mycomplex.users.domain.model.PropertyRole;
import com.mycomplex.users.infrastructure.adapter.out.persistence.dbo.AssignedPropertyDbo;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface AssignedPropertyPersistenceMapper {

    @Mapping(
            target = "role",
            source = "role",
            qualifiedByName = "mapRole"
    )
    AssignedProperty toDomain(AssignedPropertyDbo dbo);

    @Named("mapRole")
    default PropertyRole mapRole(String databaseRole) {
        return switch (databaseRole) {
            case "ADMIN_COPROPIEDAD" -> PropertyRole.PROPERTY_ADMIN;
            case "PROPIETARIO" -> PropertyRole.OWNER;
            case "RESIDENTE" -> PropertyRole.RESIDENT;
            case "VIGILANTE" -> PropertyRole.SECURITY_GUARD;
            case "TRABAJADOR" -> PropertyRole.WORKER;
            default -> throw new IllegalArgumentException(
                    "Unsupported database role: " + databaseRole
            );
        };
    }
}