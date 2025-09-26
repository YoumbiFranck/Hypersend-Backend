package com.thm_modul.register_user;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );

    // Username validation pattern (alphanumeric + underscore, 3-50 chars)
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_]{3,50}$"
    );

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Register a new user with comprehensive validation
     * Now includes email and username validation as requested in TODOs
     */
    @Transactional
    public void registerUser(UserRegistrationRequest request) {
        // Validate email format
        validateEmail(request.email());

        // Validate username format
        validateUsername(request.userName());

        // Validate password strength
        validatePassword(request.password());

        // Check if email is already taken
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        // Check if username is already taken
        if (userRepository.existsByUserName(request.userName())) {
            throw new IllegalArgumentException("Username is already taken");
        }

        try {
            // Hash the password
            String hashedPassword = passwordEncoder.encode(request.password());

            // Create user entity
            User user = User.builder()
                    .userName(request.userName())
                    .email(request.email())
                    .password(hashedPassword)
                    .build();

            // Save to database
            userRepository.save(user);

        } catch (DataIntegrityViolationException e) {
            // Handle race condition where user might be created between check and save
            throw new IllegalArgumentException("User with this email or username already exists");
        }
    }

    /**
     * Validate email format
     */
    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }

        if (email.length() > 255) {
            throw new IllegalArgumentException("Email is too long (max 255 characters)");
        }
    }

    /**
     * Validate username format and constraints
     */
    private void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException(
                    "Username must be 3-50 characters long and contain only letters, numbers, and underscores");
        }
    }

    /**
     * Validate password strength
     */
    private void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        if (password.length() > 255) {
            throw new IllegalArgumentException("Password is too long (max 255 characters)");
        }

        // Check for at least one letter and one number
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);

        if (!hasLetter || !hasDigit) {
            throw new IllegalArgumentException(
                    "Password must contain at least one letter and one number");
        }
    }
}