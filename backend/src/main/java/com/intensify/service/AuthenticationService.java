package com.intensify.service;

import com.intensify.dto.AuthDtos.*;
import com.intensify.entity.User;
import com.intensify.exception.AppException;
import com.intensify.repository.UserRepository;
import com.intensify.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public String register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw AppException.conflict("Email is already registered.");
        }
        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .targetRole(request.targetRole())
                .build();
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Concurrent registration race: both threads passed existsByEmail(),
            // second INSERT hit the UNIQUE constraint — return the same 409 as the normal path.
            throw AppException.conflict("Email is already registered.");
        }
        return jwtUtils.generateToken(user.getEmail());
    }

    public String login(LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
            return jwtUtils.generateToken(auth.getName());
        } catch (BadCredentialsException e) {
            throw AppException.badRequest("Invalid email or password.");
        }
    }

    @Transactional
    public void deleteAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.notFound("User not found."));
        // CascadeType.ALL on User.sessions and User.skillMetrics handles cascading deletion
        userRepository.delete(user);
    }
}
