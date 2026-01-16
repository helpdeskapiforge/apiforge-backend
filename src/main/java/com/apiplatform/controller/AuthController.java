package com.apiplatform.controller;

import com.apiplatform.model.User;
import com.apiplatform.model.Workspace;
import com.apiplatform.payload.request.LoginRequest;
import com.apiplatform.payload.request.SignupRequest;
import com.apiplatform.payload.response.JwtResponse;
import com.apiplatform.repository.UserRepository;
import com.apiplatform.repository.WorkspaceRepository;
import com.apiplatform.security.jwt.JwtUtils;
import com.apiplatform.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired AuthenticationManager authenticationManager;
    @Autowired UserRepository userRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtUtils jwtUtils;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        String fullName = userDetails.getFullName(); // or getName()

        String firstName = fullName;
        String lastName = "";

        if (fullName != null && fullName.contains(" ")) {
            int spaceIndex = fullName.indexOf(" ");
            firstName = fullName.substring(0, spaceIndex);
            lastName = fullName.substring(spaceIndex + 1);
        }


        return ResponseEntity.ok(new JwtResponse(jwt, userDetails.getId(), userDetails.getUsername(), userDetails.getEmail(), firstName, lastName));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body("Error: Email is already in use!");
        }

        User user = new User();
        user.setFullName(signUpRequest.getFullName());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));
        
        User savedUser = userRepository.save(user);

        // Create Default Workspace for new User
        Workspace defaultWorkspace = new Workspace();
        defaultWorkspace.setName("My Workspace");
        defaultWorkspace.setOwner(savedUser);
        workspaceRepository.save(defaultWorkspace);

        return ResponseEntity.ok("User registered successfully!");
    }
}