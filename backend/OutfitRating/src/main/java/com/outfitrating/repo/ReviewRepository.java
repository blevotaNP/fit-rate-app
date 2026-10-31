package com.outfitrating.repo;

import com.outfitrating.domain.Review;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByLookIdAndAuthorId(Long lookId, Long authorId);

    @EntityGraph(attributePaths = {"author"})
    Page<Review> findByLookId(Long lookId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "look", "look.brand"})
    Page<Review> findByAuthorId(Long authorId, Pageable pageable);

    /** Global review feed; prose-only so the homepage does not show bare number drops. */
    @EntityGraph(attributePaths = {"author", "look", "look.brand"})
    @Query("""
            select r from Review r
            where r.body is not null and length(trim(r.body)) > 0
            """)
    Page<Review> findProseFeed(Pageable pageable);

    long countByAuthorId(Long authorId);

    @Query("""
            select count(r.id) from Review r
            where r.author.id = :authorId and r.body is not null and length(trim(r.body)) > 0
            """)
    long countProseByAuthor(@Param("authorId") Long authorId);

    @Query("select coalesce(avg(r.score), 0) from Review r where r.author.id = :authorId")
    double averageScoreGivenBy(@Param("authorId") Long authorId);

    /**
     * Recomputes every denormalised aggregate for one look in a single pass.
     * Returned as a projection so the service can copy it straight onto the entity.
     */
    @Query("""
            select count(r.id) as ratingsCount,
                   sum(case when r.body is not null and length(trim(r.body)) > 0 then 1 else 0 end) as reviewsCount,
                   avg(r.score) as avgScore,
                   avg(r.style) as avgStyle,
                   avg(r.fit) as avgFit,
                   avg(r.color) as avgColor,
                   avg(r.originality) as avgOriginality,
                   avg(r.quality) as avgQuality
            from Review r
            where r.look.id = :lookId
            """)
    ScoreAggregate aggregateForLook(@Param("lookId") Long lookId);

    /** Ids of looks the user has already judged — the exclusion set for recommendations. */
    @Query("select r.look.id from Review r where r.author.id = :authorId")
    List<Long> findRatedLookIds(@Param("authorId") Long authorId);

    /** Tags of the looks this user scored highly — the taste profile seed. */
    @Query("""
            select t.id
            from Review r
            join r.look l
            join l.tags t
            where r.author.id = :authorId and r.score >= :minScore
            """)
    List<Long> findPreferredTagIds(@Param("authorId") Long authorId, @Param("minScore") int minScore);

    List<Review> findByIdIn(Collection<Long> ids);
}
