package com.enterprise.payment.services;

import com.enterprise.payment.entities.AuditAction;

public interface AuditService {
    public void log(AuditAction action, String entityId,
                    String userId, String ipAddress, String details);

}

