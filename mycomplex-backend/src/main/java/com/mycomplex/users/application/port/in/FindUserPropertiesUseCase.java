package com.mycomplex.users.application.port.in;

import java.util.List;

import com.mycomplex.users.domain.model.AssignedProperty;

public interface FindUserPropertiesUseCase {

    List<AssignedProperty> findByAuth0Subject(String auth0Subject);
}