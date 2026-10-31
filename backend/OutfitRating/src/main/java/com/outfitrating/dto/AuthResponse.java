package com.outfitrating.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UserProfileDto user) {

    public static AuthResponse of(String accessToken, long expiresInSeconds, UserProfileDto user) {
        return new AuthResponse(accessToken, "Bearer", expiresInSeconds, user);
    }
}
