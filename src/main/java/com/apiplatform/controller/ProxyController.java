package com.apiplatform.controller;

import com.apiplatform.model.User;
import com.apiplatform.payload.request.ProxyRequestDto;
import com.apiplatform.payload.response.ProxyResponseDto;
import com.apiplatform.security.access.CurrentUserProvider;
import com.apiplatform.service.ProxyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/proxy", "/api/proxy"}) // legacy "/api/proxy" kept temporarily; see CHANGELOG.md
public class ProxyController {

    private final ProxyService proxyService;
    private final CurrentUserProvider currentUserProvider;

    public ProxyController(ProxyService proxyService, CurrentUserProvider currentUserProvider) {
        this.proxyService = proxyService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/execute")
    public ResponseEntity<ProxyResponseDto> executeRequest(@RequestBody ProxyRequestDto requestDto) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(proxyService.execute(requestDto, user));
    }
}
