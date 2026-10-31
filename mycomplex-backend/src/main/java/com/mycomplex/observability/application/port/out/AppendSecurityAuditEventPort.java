package com.mycomplex.observability.application.port.out;

import com.mycomplex.observability.domain.SecurityAuditEvent;

public interface AppendSecurityAuditEventPort {

    void append(SecurityAuditEvent event);
}
