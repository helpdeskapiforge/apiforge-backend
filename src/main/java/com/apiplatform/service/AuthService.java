package com.apiplatform.service;

import com.apiplatform.exception.TooManyRequestsException;
import com.apiplatform.model.User;
import com.apiplatform.model.Workspace;
import com.apiplatform.payload.request.LoginRequest;
import com.apiplatform.payload.request.SignupRequest;
import com.apiplatform.payload.response.JwtResponse;
import com.apiplatform.repository.UserRepository;
import com.apiplatform.repository.WorkspaceRepository;
import com.apiplatform.security.jwt.JwtUtils;
import com.apiplatform.security.ratelimit.LoginRateLimiter;
import com.apiplatform.security.services.UserDetailsImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final LoginRateLimiter loginRateLimiter;

    public AuthService(AuthenticationManager authenticationManager,
                        UserRepository userRepository,
                        WorkspaceRepository workspaceRepository,
                        PasswordEncoder encoder,
                        JwtUtils jwtUtils,
                        LoginRateLimiter loginRateLimiter) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
        this.loginRateLimiter = loginRateLimiter;
    }

    public JwtResponse signIn(LoginRequest loginRequest, String clientIp) {
        String rateLimitKey = clientIp + ":" + loginRequest.getEmail().toLowerCase();
        if (!loginRateLimiter.tryAcquire(rateLimitKey)) {
            throw new TooManyRequestsException("Too many login attempts. Please try again in a minute.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        String fullName = userDetails.getFullName();
        String firstName = fullName;
        String lastName = "";
        if (fullName != null && fullName.contains(" ")) {
            int spaceIndex = fullName.indexOf(' ');
            firstName = fullName.substring(0, spaceIndex);
            lastName = fullName.substring(spaceIndex + 1);
        }

        return new JwtResponse(jwt, userDetails.getId(), userDetails.getUsername(), userDetails.getEmail(), firstName, lastName);
    }

    @Transactional
    public void signUp(SignupRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            // Deliberately vague to avoid confirming account existence would be ideal,
            // but the frontend needs to tell users to log in instead — kept as a
            // documented trade-off rather than a silent inconsistency.
            throw new IllegalArgumentException("Email is already in use.");
        }

        User user = new User();
        user.setFullName(signUpRequest.getFullName());
        user.setEmail(signUpRequest.getEmail().toLowerCase());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));
        User savedUser = userRepository.save(user);

        Workspace defaultWorkspace = new Workspace();
        defaultWorkspace.setName("My Workspace");
        defaultWorkspace.setOwner(savedUser);
        workspaceRepository.save(defaultWorkspace);
    }
}
