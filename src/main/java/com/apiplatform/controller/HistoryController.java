package com.apiplatform.controller;

import com.apiplatform.model.RequestHistory;
import com.apiplatform.model.User;
import com.apiplatform.repository.RequestHistoryRepository;
import com.apiplatform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/history")
public class HistoryController {

    @Autowired RequestHistoryRepository historyRepository;
    @Autowired UserRepository userRepository;

    @GetMapping("/me")
    public List<RequestHistory> getMyHistory() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return historyRepository.findTop50ByUserIdOrderByTimestampDesc(user.getId());
    }
}