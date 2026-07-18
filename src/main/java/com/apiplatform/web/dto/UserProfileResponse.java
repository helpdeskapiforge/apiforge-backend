package com.apiplatform.web.dto;

import com.apiplatform.model.User;

public record UserProfileResponse(Long id, String email, String fullName, String avatarUrl) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getFullName(), user.getAvatarUrl());
    }
}
