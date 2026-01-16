package com.apiplatform.controller;

import com.apiplatform.model.User;
import com.apiplatform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder encoder;

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest request) {
        // Get currently logged-in user
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update Full Name
        if (request.fullName != null && !request.fullName.isEmpty()) {
            user.setFullName(request.fullName);
        }

        // Update Password (only if provided)
        if (request.password != null && !request.password.isEmpty()) {
            user.setPassword(encoder.encode(request.password));
        }

        userRepository.save(user);
        return ResponseEntity.ok("Profile updated successfully");
    }

    // DTO Helper Class
    public static class UpdateProfileRequest {
        public String fullName;
        public String password;
    }
}