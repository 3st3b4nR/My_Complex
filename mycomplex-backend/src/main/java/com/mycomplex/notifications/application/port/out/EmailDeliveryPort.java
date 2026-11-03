package com.mycomplex.notifications.application.port.out;

import com.mycomplex.notifications.domain.EmailDeliveryResult;
import com.mycomplex.notifications.domain.EmailOutboxEvent;

public interface EmailDeliveryPort {

    EmailDeliveryResult deliver(EmailOutboxEvent event);
}
