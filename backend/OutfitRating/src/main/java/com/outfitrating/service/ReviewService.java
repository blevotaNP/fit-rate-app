package com.outfitrating.service;

import com.outfitrating.domain.Look;
import com.outfitrating.domain.Review;
import com.outfitrating.domain.ReviewLike;
import com.outfitrating.domain.User;
import com.outfitrating.dto.PageResponse;
import com.outfitrating.dto.ReviewDto;
import com.outfitrating.dto.SaveReviewRequest;
import com.outfitrating.exception.BadRequestException;
import com.outfitrating.exception.ForbiddenException;
import com.outfitrating.exception.NotFoundException;
import com.outfitrating.repo.LookRepository;
import com.outfitrating.repo.ReviewLikeRepository;
import com.outfitrating.repo.ReviewRepository;
import com.outfitrating.repo.ScoreAggregate;
import com.outfitrating.security.AppUserDetails;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private final ReviewRepository reviews;
    private final ReviewLikeRepository reviewLikes;
    private final LookRepository looks;
    private final ScoreCalculator scoreCalculator;
    private final DtoMapper mapper;

    public ReviewService(ReviewRepository reviews,
                         ReviewLikeRepository reviewLikes,
                         LookRepository looks,
                         ScoreCalculator scoreCalculator,
                         DtoMapper mapper) {
        this.reviews = reviews;
        this.reviewLikes = reviewLikes;
        this.looks = looks;
        this.scoreCalculator = scoreCalculator;
        this.mapper = mapper;
    }

    /**
     * Creates the review, or overwrites the one this user already left. One verdict per person per
     * look is the whole premise of the score, so this is an upsert rather than an append.
     */
    @Transactional
    public ReviewDto save(Long lookId, SaveReviewRequest request, User author) {
        Look look = looks.findById(lookId).orElseThrow(() -> NotFoundException.of("Look", lookId));
        if (look.getAuthor().getId().equals(author.getId())) {
            throw new BadRequestException("You cannot rate a look you submitted yourself");
        }

        Review review = reviews.findByLookIdAndAuthorId(lookId, author.getId())
                .orElseGet(() -> {
                    Review fresh = new Review();
                    fresh.setLook(look);
                    fresh.setAuthor(author);
                    return fresh;
                });

        review.setStyle(request.style());
        review.setFit(request.fit());
        review.setColor(request.color());
        review.setOriginality(request.originality());
        review.setQuality(request.quality());
        review.setScore(scoreCalculator.compose(request));
        review.setTitle(blankToNull(request.title()));
        review.setBody(blankToNull(request.body()));
        reviews.save(review);

        recomputeAggregates(look);
        return mapper.toDto(review, false, author.getId(), false);
    }

    @Transactional
    public void delete(Long reviewId, AppUserDetails viewer) {
        Review review = reviews.findById(reviewId)
                .orElseThrow(() -> NotFoundException.of("Review", reviewId));
        if (!viewer.isAdmin() && !viewer.id().equals(review.getAuthor().getId())) {
            throw new ForbiddenException("Only the author or an admin can delete this review");
        }
        Look look = review.getLook();
        reviews.delete(review);
        reviews.flush();
        recomputeAggregates(look);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> forLook(Long lookId, int page, int size, String sort, Long viewerId) {
        Pageable pageable = PageRequest.of(page, size, sortFor(sort));
        Page<Review> result = reviews.findByLookId(lookId, pageable);
        return toPage(result, viewerId, false);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> byAuthor(Long authorId, int page, int size, Long viewerId) {
        Page<Review> result = reviews.findByAuthorId(authorId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return toPage(result, viewerId, true);
    }

    /** Latest written reviews across the whole platform — the homepage feed. */
    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> feed(int page, int size, Long viewerId) {
        Page<Review> result = reviews.findProseFeed(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return toPage(result, viewerId, true);
    }

    @Transactional
    public int toggleLike(Long reviewId, User user) {
        Review review = reviews.findById(reviewId)
                .orElseThrow(() -> NotFoundException.of("Review", reviewId));
        if (review.getAuthor().getId().equals(user.getId())) {
            throw new BadRequestException("You cannot like your own review");
        }
        reviewLikes.findByReviewIdAndUserId(reviewId, user.getId())
                .ifPresentOrElse(reviewLikes::delete, () -> {
                    ReviewLike like = new ReviewLike();
                    like.setReview(review);
                    like.setUser(user);
                    reviewLikes.save(like);
                });
        reviewLikes.flush();
        int total = (int) reviewLikes.countByReviewId(reviewId);
        review.setLikesCount(total);
        return total;
    }

    /**
     * Rewrites the denormalised counters on a look from the authoritative review rows.
     * Called after every write so the catalogue never serves a stale average.
     */
    @Transactional
    public void recomputeAggregates(Look look) {
        ScoreAggregate aggregate = reviews.aggregateForLook(look.getId());
        long ratings = aggregate == null ? 0 : aggregate.getRatingsCount();
        look.setRatingsCount((int) ratings);
        if (ratings == 0) {
            look.setReviewsCount(0);
            look.setAvgScore(null);
            look.setAvgStyle(null);
            look.setAvgFit(null);
            look.setAvgColor(null);
            look.setAvgOriginality(null);
            look.setAvgQuality(null);
            return;
        }
        look.setReviewsCount(aggregate.getReviewsCount() == null
                ? 0
                : aggregate.getReviewsCount().intValue());
        look.setAvgScore(round(aggregate.getAvgScore()));
        look.setAvgStyle(round(aggregate.getAvgStyle()));
        look.setAvgFit(round(aggregate.getAvgFit()));
        look.setAvgColor(round(aggregate.getAvgColor()));
        look.setAvgOriginality(round(aggregate.getAvgOriginality()));
        look.setAvgQuality(round(aggregate.getAvgQuality()));
    }

    private PageResponse<ReviewDto> toPage(Page<Review> result, Long viewerId, boolean includeLook) {
        Map<Long, Boolean> liked = likeFlags(result.getContent(), viewerId);
        return PageResponse.from(result,
                review -> mapper.toDto(review, liked.get(review.getId()), viewerId, includeLook));
    }

    private Map<Long, Boolean> likeFlags(List<Review> page, Long viewerId) {
        if (viewerId == null || page.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = page.stream().map(Review::getId).toList();
        Set<Long> liked = Set.copyOf(reviewLikes.findLikedReviewIds(viewerId, ids));
        return ids.stream().collect(Collectors.toMap(Function.identity(), liked::contains));
    }

    private static Sort sortFor(String sort) {
        return switch (sort == null ? "" : sort) {
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "helpful" -> Sort.by(Sort.Direction.DESC, "likesCount")
                    .and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case "highest" -> Sort.by(Sort.Direction.DESC, "score");
            case "lowest" -> Sort.by(Sort.Direction.ASC, "score");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private static Double round(Double value) {
        return value == null ? null : Math.round(value * 10) / 10.0;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
