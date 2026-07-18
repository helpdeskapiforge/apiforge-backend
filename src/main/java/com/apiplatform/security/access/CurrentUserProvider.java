package com.apiplatform.security.access;

import com.apiplatform.exception.ResourceNotFoundException;
import com.apiplatform.model.User;
import com.apiplatform.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Every controller used to duplicate this lookup:
 * {@code userRepository.findByEmail(SecurityContextHolder...getAuthentication().getName())}
 * That's error-prone and hard to test. Centralizing it here also gives us a single
 * place to change if the "current user" resolution strategy ever changes (e.g. to
 * read the id directly from JWT claims instead of the database).
 */
@Component
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResourceNotFoundException("No authenticated user in context.");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user no longer exists: " + email));
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
