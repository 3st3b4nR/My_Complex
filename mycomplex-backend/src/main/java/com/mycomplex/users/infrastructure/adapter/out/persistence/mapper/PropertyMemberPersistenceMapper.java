package com.mycomplex.users.infrastructure.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import com.mycomplex.users.domain.model.MembershipStatus;
import com.mycomplex.users.domain.model.PropertyMember;
import com.mycomplex.users.domain.model.PropertyMemberUnit;
import com.mycomplex.users.domain.model.PropertyRole;
import com.mycomplex.properties.domain.model.PropertyStructureReference;
import com.mycomplex.properties.domain.model.PropertyStructureType;
import com.mycomplex.properties.domain.model.ResidentialUnitReference;
import com.mycomplex.properties.domain.model.ResidentialUnitType;
import com.mycomplex.users.domain.model.ResidentialRelationshipStatus;
import com.mycomplex.users.domain.model.ResidentialRelationshipType;
import com.mycomplex.users.domain.model.UserStatus;
import com.mycomplex.users.infrastructure.adapter.out.persistence.dbo.PropertyMemberDbo;
import com.mycomplex.users.infrastructure.adapter.out.persistence.dbo.PropertyMemberUnitDbo;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface PropertyMemberPersistenceMapper {

    @Mapping(
            target = "role",
            source = "role",
            qualifiedByName = "mapRole"
    )
    @Mapping(
            target = "userStatus",
            source = "userStatus",
            qualifiedByName = "mapUserStatus"
    )
    @Mapping(
            target = "membershipStatus",
            source = "membershipStatus",
            qualifiedByName = "mapMembershipStatus"
    )
    PropertyMember toDomain(PropertyMemberDbo dbo);

    default PropertyMemberUnit toDomain(PropertyMemberUnitDbo dbo) {
        PropertyStructureReference structure =
                dbo.structureName() == null
                        ? null
                        : new PropertyStructureReference(
                                dbo.structureName(),
                                mapStructureType(dbo.structureType())
                        );

        ResidentialUnitReference unit = new ResidentialUnitReference(
                dbo.unitId(),
                structure,
                dbo.unitNumber(),
                mapUnitType(dbo.unitType())
        );

        return new PropertyMemberUnit(
                unit,
                mapRelationshipType(dbo.relationshipType()),
                dbo.primary(),
                mapResidentialStatus(dbo.status())
        );
    }

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

    @Named("mapUserStatus")
    default UserStatus mapUserStatus(String databaseStatus) {
        return switch (databaseStatus) {
            case "ACTIVO" -> UserStatus.ACTIVE;
            case "INACTIVO" -> UserStatus.INACTIVE;
            case "BLOQUEADO" -> UserStatus.BLOCKED;
            default -> throw new IllegalArgumentException(
                    "Unsupported database user status: "
                            + databaseStatus
            );
        };
    }

    @Named("mapMembershipStatus")
    default MembershipStatus mapMembershipStatus(
            String databaseStatus
    ) {
        return switch (databaseStatus) {
            case "ACTIVA" -> MembershipStatus.ACTIVE;
            case "INACTIVA" -> MembershipStatus.INACTIVE;
            case "SUSPENDIDA" -> MembershipStatus.SUSPENDED;
            default -> throw new IllegalArgumentException(
                    "Unsupported database membership status: "
                            + databaseStatus
            );
        };
    }

    @Named("mapStructureType")
    default PropertyStructureType mapStructureType(
            String databaseType
    ) {
        if (databaseType == null) {
            return null;
        }

        return switch (databaseType) {
            case "TORRE" -> PropertyStructureType.TOWER;
            case "BLOQUE" -> PropertyStructureType.BLOCK;
            case "SECTOR" -> PropertyStructureType.SECTOR;
            case "OTRO" -> PropertyStructureType.OTHER;
            default -> throw new IllegalArgumentException(
                    "Unsupported database structure type: "
                            + databaseType
            );
        };
    }

    @Named("mapUnitType")
    default ResidentialUnitType mapUnitType(String databaseType) {
        return switch (databaseType) {
            case "APARTAMENTO" -> ResidentialUnitType.APARTMENT;
            case "CASA" -> ResidentialUnitType.HOUSE;
            case "LOCAL" -> ResidentialUnitType.COMMERCIAL_SPACE;
            case "OFICINA" -> ResidentialUnitType.OFFICE;
            case "OTRO" -> ResidentialUnitType.OTHER;
            default -> throw new IllegalArgumentException(
                    "Unsupported database unit type: " + databaseType
            );
        };
    }

    @Named("mapRelationshipType")
    default ResidentialRelationshipType mapRelationshipType(
            String databaseType
    ) {
        return switch (databaseType) {
            case "PROPIETARIO" -> ResidentialRelationshipType.OWNER;
            case "RESIDENTE" -> ResidentialRelationshipType.RESIDENT;
            default -> throw new IllegalArgumentException(
                    "Unsupported database relationship type: "
                            + databaseType
            );
        };
    }

    @Named("mapResidentialStatus")
    default ResidentialRelationshipStatus mapResidentialStatus(
            String databaseStatus
    ) {
        return switch (databaseStatus) {
            case "ACTIVA" -> ResidentialRelationshipStatus.ACTIVE;
            case "INACTIVA" -> ResidentialRelationshipStatus.INACTIVE;
            case "FINALIZADA" -> ResidentialRelationshipStatus.ENDED;
            default -> throw new IllegalArgumentException(
                    "Unsupported database residential status: "
                            + databaseStatus
            );
        };
    }
}
