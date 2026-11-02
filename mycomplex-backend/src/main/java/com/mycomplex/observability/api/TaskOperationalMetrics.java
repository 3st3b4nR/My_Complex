package com.mycomplex.observability.api;

public interface TaskOperationalMetrics {
    void taskCreated(String priority);
    void taskAssignment(boolean reassignment);
    void taskStatusChanged(String status);
    void fcmDelivery(String result);
    void fcmDeliveryFailure(String reason);
    void fcmDeliveryRetry(String reason);
}
