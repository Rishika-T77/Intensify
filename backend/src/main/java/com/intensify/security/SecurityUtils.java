package com.intensify.security;

import com.intensify.entity.User;
import com.intensify.exception.AppException;
import com.intensify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Utility to resolve the authenticated User entity from the SecurityContext.
 * Centralizes the pattern: SecurityContext → email → DB lookup.
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email;
        if (principal instanceof UserDetails ud) {
            email = ud.getUsername();
        } else {
            email = principal.toString();
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.notFound("Authenticated user not found."));
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
