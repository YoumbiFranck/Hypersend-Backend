package com.thm_modul.register_user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void registerUser(UserRegistrationRequest request) {
        // Passwort hashen
        String hashedPassword = passwordEncoder.encode(request.password());

        User user = User.builder()
                .userName(request.userName())
                .email(request.email())
                .password(hashedPassword) // Speichere nur das gehashte Passwort!
                .build();

        //todo: check if email is valid
        //todo: check if email is not taken

        userRepository.save(user);
    }
}