package com.voiceassess.backend.repository;

import com.voiceassess.backend.model.SystemErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface SystemErrorLogRepository extends JpaRepository<SystemErrorLog, UUID> {
}
