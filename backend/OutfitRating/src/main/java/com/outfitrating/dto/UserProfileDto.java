package com.outfitrating.dto;

import com.outfitrating.domain.Role;
import java.time.Instant;

public record UserProfileDto(
        Long id,
        String username,
        String displayName,
        String email,
        String avatarUrl,
        String bio,
        String location,
        Role role,
        Instant joinedAt,
        UserStatsDto stats,
        Boolean followedByMe) {
}
