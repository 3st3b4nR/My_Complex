package com.mycomplex.users.infrastructure.adapter.in.rest.dto;

import java.util.List;

import com.mycomplex.users.domain.model.MembershipStatus;
import com.mycomplex.users.domain.model.PropertyRole;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdatePropertyMemberRequest(
        @NotNull PropertyRole role,
        @NotNull MembershipStatus membershipStatus,
        @NotNull @PositiveOrZero Long version,
        @NotNull List<@Valid UpdatePropertyMemberUnitRequest> units
) {
}
