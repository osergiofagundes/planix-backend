package com.sergio.planix.auth.dto;

import com.sergio.planix.auth.User;

public final class AvatarUrl {

    private AvatarUrl() {}

    public static String of(User user) {
        return user == null || user.getAvatarPath() == null
                ? null
                : "/api/users/" + user.getId() + "/avatar";
    }
}
