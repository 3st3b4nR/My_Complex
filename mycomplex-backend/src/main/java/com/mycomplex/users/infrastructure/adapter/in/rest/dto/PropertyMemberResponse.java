package com.mycomplex.users.infrastructure.adapter.in.rest.dto;

import java.util.UUID;

public record PropertyMemberResponse(
        UUID userId,
        String firstNames,
        String lastNames,
        String email,
        String role,
        String userStatus,
        String membershipStatus
) {
}
