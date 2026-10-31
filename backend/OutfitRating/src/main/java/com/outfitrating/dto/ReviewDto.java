package com.outfitrating.dto;

import java.time.Instant;

public record ReviewDto(
        Long id,
        UserSummaryDto author,
        LookSummaryDto look,
        short style,
        short fit,
        short color,
        short originality,
        short quality,
        short score,
        String title,
        String body,
        int likesCount,
        Instant createdAt,
        Instant updatedAt,
        Boolean likedByMe,
        boolean editable) {
}
