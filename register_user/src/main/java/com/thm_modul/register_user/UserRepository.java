package com.thm_modul.register_user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * Check if a user exists with the given email
     * Used for validation during registration
     */
    boolean existsByEmail(String email);

    /**
     * Check if a user exists with the given username
     * Used for validation during registration
     */
    boolean existsByUserName(String userName);
}
