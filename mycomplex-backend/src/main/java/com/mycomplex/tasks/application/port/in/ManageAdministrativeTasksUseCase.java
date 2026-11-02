package com.mycomplex.tasks.application.port.in;

import java.util.UUID;

import com.mycomplex.tasks.domain.AdministrativeTask;
import com.mycomplex.tasks.domain.CreateAdministrativeTask;
import com.mycomplex.tasks.domain.TaskStatus;
import com.mycomplex.tasks.domain.UpdateAdministrativeTask;

public interface ManageAdministrativeTasksUseCase {
    AdministrativeTask create(CreateAdministrativeTask command,
            String auth0Subject);
    AdministrativeTask update(UpdateAdministrativeTask command,
            String auth0Subject);
    AdministrativeTask changeStatus(UUID propertyId, UUID taskId,
            TaskStatus status, long version, String auth0Subject);
}
