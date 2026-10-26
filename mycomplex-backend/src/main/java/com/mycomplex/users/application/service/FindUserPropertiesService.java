package com.mycomplex.users.application.service;

import java.util.List;

import com.mycomplex.users.application.port.in.FindUserPropertiesUseCase;
import com.mycomplex.users.application.port.out.LoadUserPropertiesPort;
import com.mycomplex.users.domain.model.AssignedProperty;

public final class FindUserPropertiesService implements FindUserPropertiesUseCase {

	private final LoadUserPropertiesPort loadUserPropertiesPort;

	public FindUserPropertiesService(LoadUserPropertiesPort loadUserPropertiesPort) {
		this.loadUserPropertiesPort = loadUserPropertiesPort;
	}

	@Override
	public List<AssignedProperty> findByAuth0Subject(String auth0Subject) {
		if (auth0Subject == null || auth0Subject.isBlank()) {
			throw new IllegalArgumentException("auth0Subject must not be blank");
		}

		return loadUserPropertiesPort.loadActiveByAuth0Subject(auth0Subject);
	}
}