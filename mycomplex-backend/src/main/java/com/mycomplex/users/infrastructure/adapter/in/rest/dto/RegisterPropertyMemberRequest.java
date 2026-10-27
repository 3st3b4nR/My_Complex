package com.mycomplex.users.infrastructure.adapter.in.rest.dto;

import java.util.List;
import java.util.UUID;

import com.mycomplex.users.domain.model.MembershipStatus;
import com.mycomplex.users.domain.model.PropertyMemberRegistrationMode;
import com.mycomplex.users.domain.model.PropertyRole;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterPropertyMemberRequest(
        @NotNull PropertyMemberRegistrationMode mode,
        UUID userId,
        UUID membershipId,
        @Size(max = 120) String firstNames,
        @Size(max = 120) String lastNames,
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 20) String documentType,
        @Size(max = 50) String documentNumber,
        @NotNull PropertyRole role,
        @NotNull MembershipStatus membershipStatus,
        @NotNull List<@Valid UpdatePropertyMemberUnitRequest> units
) {
}
