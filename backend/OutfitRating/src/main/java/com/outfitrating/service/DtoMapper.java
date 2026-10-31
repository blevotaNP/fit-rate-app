package com.outfitrating.service;

import com.outfitrating.domain.Brand;
import com.outfitrating.domain.Comment;
import com.outfitrating.domain.Look;
import com.outfitrating.domain.Review;
import com.outfitrating.domain.Tag;
import com.outfitrating.domain.User;
import com.outfitrating.dto.BrandDto;
import com.outfitrating.dto.CommentDto;
import com.outfitrating.dto.LookSummaryDto;
import com.outfitrating.dto.ReviewDto;
import com.outfitrating.dto.ScoreBreakdownDto;
import com.outfitrating.dto.TagDto;
import com.outfitrating.dto.UserSummaryDto;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Entity to DTO translation, kept in one place so the wire format never drifts between endpoints.
 *
 * <p>Viewer-dependent flags ({@code favoritedByMe}, {@code likedByMe}, {@code editable}) are passed
 * in rather than looked up here: callers resolve them once per page with a single query instead of
 * once per row.
 */
@Component
public class DtoMapper {

    public UserSummaryDto toSummary(User user) {
        if (user == null) {
            return null;
        }
        return new UserSummaryDto(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getRole());
    }

    public BrandDto toDto(Brand brand) {
        if (brand == null) {
            return null;
        }
        return new BrandDto(
                brand.getId(),
                brand.getName(),
                brand.getSlug(),
                brand.getCountry(),
                brand.getLogoUrl(),
                brand.getDescription());
    }

    public TagDto toDto(Tag tag) {
        return TagDto.of(tag.getId(), tag.getName(), tag.getSlug());
    }

    public List<TagDto> toTagDtos(Look look) {
        return look.getTags().stream().map(this::toDto).toList();
    }

    public LookSummaryDto toSummary(Look look, Boolean favoritedByMe) {
        return new LookSummaryDto(
                look.getId(),
                look.getSlug(),
                look.getTitle(),
                look.getType(),
                look.getCategory(),
                look.getSeason(),
                look.getImageUrl(),
                toDto(look.getBrand()),
                toSummary(look.getAuthor()),
                look.getPriceCents(),
                look.getCurrency(),
                look.isFeatured(),
                look.getRatingsCount(),
                look.getReviewsCount(),
                look.getAvgScore(),
                toTagDtos(look),
                look.getCreatedAt(),
                favoritedByMe);
    }

    public ScoreBreakdownDto toBreakdown(Look look) {
        if (look.getRatingsCount() == 0) {
            return ScoreBreakdownDto.EMPTY;
        }
        return new ScoreBreakdownDto(
                look.getAvgStyle(),
                look.getAvgFit(),
                look.getAvgColor(),
                look.getAvgOriginality(),
                look.getAvgQuality(),
                look.getAvgScore());
    }

    public ReviewDto toDto(Review review, Boolean likedByMe, Long viewerId, boolean includeLook) {
        return new ReviewDto(
                review.getId(),
                toSummary(review.getAuthor()),
                includeLook ? toSummary(review.getLook(), null) : null,
                review.getStyle(),
                review.getFit(),
                review.getColor(),
                review.getOriginality(),
                review.getQuality(),
                review.getScore(),
                review.getTitle(),
                review.getBody(),
                review.getLikesCount(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                likedByMe,
                viewerId != null && viewerId.equals(review.getAuthor().getId()));
    }

    public CommentDto toDto(Comment comment, Long viewerId, List<CommentDto> replies) {
        return new CommentDto(
                comment.getId(),
                toSummary(comment.getAuthor()),
                comment.getBody(),
                comment.getCreatedAt(),
                viewerId != null && viewerId.equals(comment.getAuthor().getId()),
                replies == null ? List.of() : replies);
    }
}
