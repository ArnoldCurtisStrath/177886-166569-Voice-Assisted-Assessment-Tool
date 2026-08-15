package com.voiceassess.backend.repository;

import com.voiceassess.backend.model.Administrator;
import com.voiceassess.backend.model.School;
import com.voiceassess.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface AdministratorRepository extends JpaRepository<Administrator, UUID> {
    Optional<Administrator> findByUser(User user);
    Optional<Administrator> findByUserUserId(UUID userId);
    List<Administrator> findBySchool(School school);
}
