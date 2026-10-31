package com.mycomplex.observability.api;

public interface RecordSecurityAuditUseCase {

    void record(SecurityAuditCommand command);
}
