package com.voiceassess.backend.repository;

import com.voiceassess.backend.model.Capability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CapabilityRepository extends JpaRepository<Capability, UUID> {
    Optional<Capability> findByPermissionFlag(String permissionFlag);
}
