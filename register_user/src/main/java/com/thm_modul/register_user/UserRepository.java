package com.thm_modul.register_user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
    // Additional query methods can be defined here if needed
    // For example, to find by email:
    // Optional<User> findByEmail(String email);
}
