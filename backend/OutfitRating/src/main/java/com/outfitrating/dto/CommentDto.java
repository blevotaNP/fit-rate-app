package com.outfitrating.dto;

import java.time.Instant;
import java.util.List;

public record CommentDto(
        Long id,
        UserSummaryDto author,
        String body,
        Instant createdAt,
        boolean editable,
        List<CommentDto> replies) {
}
