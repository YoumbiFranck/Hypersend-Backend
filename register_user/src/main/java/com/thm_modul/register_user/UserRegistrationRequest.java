package com.thm_modul.register_user;


public record UserRegistrationRequest(String userName, String email, String password) {
    // This record is used to encapsulate the user registration data
    // It includes fields for userName, email, and password
    // Lombok annotations are not needed here as records automatically generate getters and toString methods
}