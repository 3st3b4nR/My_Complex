package com.mycomplex.users.infrastructure.adapter.out.persistence.dbo;

import java.util.UUID;

public interface PropertyMemberRow {

    UUID getUserId();

    String getFirstNames();

    String getLastNames();

    String getEmail();

    String getRole();

    String getUserStatus();

    String getMembershipStatus();

    UUID getUnitId();

    String getStructureName();

    String getStructureType();

    String getUnitNumber();

    String getUnitType();

    String getRelationshipType();

    Boolean getPrimaryUnit();

    String getResidentialStatus();
}
