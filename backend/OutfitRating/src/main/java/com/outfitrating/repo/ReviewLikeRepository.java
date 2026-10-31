package com.outfitrating.repo;

import com.outfitrating.domain.ReviewLike;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {

    Optional<ReviewLike> findByReviewIdAndUserId(Long reviewId, Long userId);

    long countByReviewId(Long reviewId);

    /** Which of these reviews the given user has already liked — one query per page of reviews. */
    @Query("select rl.review.id from ReviewLike rl where rl.user.id = :userId and rl.review.id in :reviewIds")
    List<Long> findLikedReviewIds(@Param("userId") Long userId,
                                  @Param("reviewIds") Collection<Long> reviewIds);

    @Query("select count(rl.id) from ReviewLike rl where rl.review.author.id = :authorId")
    long countLikesReceivedBy(@Param("authorId") Long authorId);
}
