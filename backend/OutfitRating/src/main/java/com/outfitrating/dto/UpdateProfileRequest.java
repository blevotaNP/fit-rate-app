package com.outfitrating.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 1, max = 64) String displayName,
        @Size(max = 280) String bio,
        @Size(max = 80) String location,
        @Size(max = 512) String avatarUrl) {
}
