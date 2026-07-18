package com.apiplatform.controller;

import com.apiplatform.payload.request.LoginRequest;
import com.apiplatform.payload.request.SignupRequest;
import com.apiplatform.payload.response.JwtResponse;
import com.apiplatform.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/auth", "/api/auth"}) // legacy "/api/auth" kept temporarily; see CHANGELOG.md
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signin")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
                                                          HttpServletRequest request) {
        JwtResponse response = authService.signIn(loginRequest, clientIp(request));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    public ResponseEntity<Void> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        authService.signUp(signUpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Best-effort client IP resolution. X-Forwarded-For is only trustworthy when the
     * app sits behind a reverse proxy/load balancer that sets it itself (and strips any
     * client-supplied value) — deployment must ensure that.
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
