package com.sergio.planix.auth.dto;

public record AuthResponse(String accessToken, String refreshToken, long expiresInSeconds) {}
