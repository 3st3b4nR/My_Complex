package com.mycomplex.tasks.application.port.in;

import java.util.List;
import java.util.UUID;

import com.mycomplex.tasks.domain.AdministrativeTask;
import com.mycomplex.tasks.domain.TaskSearchCriteria;
import com.mycomplex.users.api.TaskAccessDirectory.Worker;

public interface FindAdministrativeTasksUseCase {
    List<AdministrativeTask> find(TaskSearchCriteria criteria,
            String auth0Subject);
    AdministrativeTask findOne(UUID propertyId, UUID taskId,
            String auth0Subject);
    List<Worker> findEligibleWorkers(UUID propertyId, String auth0Subject);
}
