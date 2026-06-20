package com.voiceassess.backend.model;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * RBAC permission flag. An Administrator holds a set of capabilities.
 * Simple string-based flags — "CREATE_TEACHER", "VIEW_REPORTS", etc.
 */
@Entity
@Table(name = "capabilities")
public class Capability {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "capability_id")
    private UUID capabilityId;

    @Column(name = "permission_flag", nullable = false, unique = true)
    private String permissionFlag;

    public Capability() {}

    public Capability(String permissionFlag) {
        this.permissionFlag = permissionFlag;
    }

    public UUID getCapabilityId() { return capabilityId; }
    public void setCapabilityId(UUID capabilityId) { this.capabilityId = capabilityId; }

    public String getPermissionFlag() { return permissionFlag; }
    public void setPermissionFlag(String permissionFlag) { this.permissionFlag = permissionFlag; }
}
