package com.mycomplex.users.infrastructure.adapter.out.persistence.dbo;

import java.time.Instant;
import java.util.UUID;

public interface PropertyMemberRow {

    UUID getMembershipId();

    UUID getUserId();

    String getFirstNames();

    String getLastNames();

    String getEmail();

    String getRole();

    String getUserStatus();

    String getRegistrationStatus();

    String getMembershipStatus();

    Long getMembershipVersion();

    Instant getInvitationSentAt();

    UUID getAssignmentId();

    UUID getUnitId();

    String getRelationshipType();

    Boolean getPrimaryUnit();

    String getResidentialStatus();
}
