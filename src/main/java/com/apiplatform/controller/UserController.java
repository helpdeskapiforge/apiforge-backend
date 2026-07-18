package com.apiplatform.controller;

import com.apiplatform.model.User;
import com.apiplatform.repository.UserRepository;
import com.apiplatform.security.access.CurrentUserProvider;
import com.apiplatform.web.dto.UpdateProfileRequest;
import com.apiplatform.web.dto.UserProfileResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/user", "/api/user"}) // legacy "/api/user" kept temporarily; see CHANGELOG.md
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final CurrentUserProvider currentUserProvider;

    public UserController(UserRepository userRepository, PasswordEncoder encoder, CurrentUserProvider currentUserProvider) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile() {
        return ResponseEntity.ok(UserProfileResponse.from(currentUserProvider.getCurrentUser()));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        User user = currentUserProvider.getCurrentUser();

        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(encoder.encode(request.password()));
        }

        User saved = userRepository.save(user);
        return ResponseEntity.ok(UserProfileResponse.from(saved));
    }
}
