package com.dibya.knowledgehub.audit;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(length = 100)
    private String resource;

    @Column(name = "resource_id", length = 255)
    private String resourceId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(nullable = false)
    private OffsetDateTime timestamp = OffsetDateTime.now();

    public AuditLog(UUID userId, String action, String resource, String resourceId, String ipAddress) {
        this.userId = userId;
        this.action = action;
        this.resource = resource;
        this.resourceId = resourceId;
        this.ipAddress = ipAddress;
    }
}
