package com.voiceassess.backend.repository;

import com.voiceassess.backend.model.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface SchoolRepository extends JpaRepository<School, UUID> {
}
