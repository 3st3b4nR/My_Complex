package com.mycomplex.users.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.mycomplex.users.application.port.out.LoadUserPropertiesPort;
import com.mycomplex.users.domain.model.AssignedProperty;
import com.mycomplex.users.domain.model.PropertyRole;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FindUserPropertiesServiceTest {
	@Test
	void rejectsNullAuth0Subject() {
		LoadUserPropertiesPort loadUserPropertiesPort = auth0Subject -> List.of();

		FindUserPropertiesService service = new FindUserPropertiesService(loadUserPropertiesPort);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> service.findByAuth0Subject(null));

		assertEquals("auth0Subject must not be blank", exception.getMessage());
	}

	@Test
	void rejectsBlankAuth0Subject() {
		LoadUserPropertiesPort loadUserPropertiesPort = auth0Subject -> List.of();

		FindUserPropertiesService service = new FindUserPropertiesService(loadUserPropertiesPort);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> service.findByAuth0Subject(" "));

		assertEquals("auth0Subject must not be blank", exception.getMessage());
	}

	@Test
	void returnsActivePropertiesAssignedToAuthenticatedUser() {
		String auth0Subject = "auth0|6ad6a66c5703ba3cd5629f70";

		List<AssignedProperty> expectedProperties = List
				.of(new AssignedProperty(UUID.fromString("10000000-0000-0000-0000-000000000001"), "Los Cerezos",
						"Rionegro", PropertyRole.PROPERTY_ADMIN));

		LoadUserPropertiesPort loadUserPropertiesPort = receivedSubject -> {
			assertEquals(auth0Subject, receivedSubject);

			return expectedProperties;
		};

		FindUserPropertiesService service = new FindUserPropertiesService(loadUserPropertiesPort);

		List<AssignedProperty> result = service.findByAuth0Subject(auth0Subject);

		assertEquals(expectedProperties, result);
	}
}