package com.mycomplex.users.domain.model;

import java.util.UUID;

public record AssignedProperty(
		UUID propertyId,
		String name,
		String city,
		PropertyRole role
) {
}