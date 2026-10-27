package com.mycomplex.users.infrastructure.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import com.mycomplex.users.domain.model.MembershipStatus;
import com.mycomplex.users.domain.model.PropertyMember;
import com.mycomplex.users.domain.model.PropertyRole;
import com.mycomplex.users.domain.model.UserStatus;
import com.mycomplex.users.infrastructure.adapter.out.persistence.dbo.PropertyMemberDbo;

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
}
