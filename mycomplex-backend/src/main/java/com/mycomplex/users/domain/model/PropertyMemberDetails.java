package com.mycomplex.users.domain.model;

import java.util.List;

import com.mycomplex.properties.domain.model.AvailableResidentialUnit;

public record PropertyMemberDetails(
        PropertyMember member,
        List<AvailableResidentialUnit> availableUnits
) {
}
