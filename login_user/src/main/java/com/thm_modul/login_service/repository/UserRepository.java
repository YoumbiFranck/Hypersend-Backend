package com.thm_modul.login_service.repository;

import com.thm_modul.login_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUserName(String userName);

    Optional<User> findByEmail(String email);

    Optional<User> findByUserNameOrEmail(String userName, String email);

    /**
     * Check if a user exists by ID and is enabled
     * Used for user validation by other services
     */
    boolean existsByIdAndEnabledTrue(Integer id);

    /**
     * Find user by ID only if enabled
     * Used to ensure we only return active users
     */
    Optional<User> findByIdAndEnabledTrue(Integer id);
}
