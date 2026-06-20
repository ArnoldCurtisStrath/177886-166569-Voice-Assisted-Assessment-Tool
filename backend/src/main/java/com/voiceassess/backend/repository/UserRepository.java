package com.voiceassess.backend.repository;

import com.voiceassess.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Single repository for the entire User hierarchy.
 * SINGLE_TABLE inheritance means one table covers Admins, Teachers, Students, Parents.
 * Use Spring Data JPA's type-based queries to fetch specific subtypes if needed.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    List<User> findByIsActiveTrue();
    List<User> findByIsActiveFalse();
}
