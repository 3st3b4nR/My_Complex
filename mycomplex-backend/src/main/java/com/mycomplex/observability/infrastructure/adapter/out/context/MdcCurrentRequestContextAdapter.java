package com.mycomplex.observability.infrastructure.adapter.out.context;

import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.mycomplex.observability.application.port.out.CurrentRequestContextPort;
import com.mycomplex.observability.api.CorrelationIds;

@Component
public class MdcCurrentRequestContextAdapter
        implements CurrentRequestContextPort {

    @Override
    public String correlationId() {
        String value = MDC.get(CorrelationIds.MDC_KEY);
        return value == null ? UUID.randomUUID().toString() : value;
    }
}
