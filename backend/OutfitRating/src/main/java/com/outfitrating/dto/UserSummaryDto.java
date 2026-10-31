package com.outfitrating.dto;

import com.outfitrating.domain.Role;

/** Compact author card embedded in looks, reviews and comments. */
public record UserSummaryDto(
        Long id,
        String username,
        String displayName,
        String avatarUrl,
        Role role) {
}
