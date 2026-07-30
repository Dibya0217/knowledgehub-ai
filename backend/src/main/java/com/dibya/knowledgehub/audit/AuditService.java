package com.dibya.knowledgehub.audit;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Async
    public void log(String action, String resource, String resourceId) {
        log(null, action, resource, resourceId, null);
    }

    @Async
    public void log(UUID userId, String action, String resource, String resourceId, String ipAddress) {
        auditLogRepository.save(new AuditLog(userId, action, resource, resourceId, ipAddress));
    }
}
