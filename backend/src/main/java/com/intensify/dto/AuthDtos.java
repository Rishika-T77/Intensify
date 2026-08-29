package com.intensify.dto;

import jakarta.validation.constraints.*;

public class AuthDtos {

    public record RegisterRequest(
            @NotBlank(message = "Name is required") @Size(max = 100) String name,
            @NotBlank(message = "Email is required") @Email(message = "Invalid email") @Size(max = 150) String email,
            @NotBlank(message = "Password is required") @Size(min = 8, message = "Password must be at least 8 characters") String password,
            String targetRole
    ) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record AuthResponse(Object data, String error) {
        public static AuthResponse success(String token) {
            return new AuthResponse(new TokenData(token), null);
        }
        public static AuthResponse error(String msg) {
            return new AuthResponse(null, msg);
        }
        public record TokenData(String token) {}
    }
}
