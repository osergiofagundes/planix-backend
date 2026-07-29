package com.sergio.planix.auth.dto;

import com.sergio.planix.auth.User;

public record UserSummary(Long id, String name) {

    public static UserSummary from(User user) {
        return user == null ? null : new UserSummary(user.getId(), user.getName());
    }
}
