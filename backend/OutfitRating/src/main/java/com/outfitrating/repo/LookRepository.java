package com.outfitrating.repo;

import com.outfitrating.domain.Look;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LookRepository extends JpaRepository<Look, Long>, JpaSpecificationExecutor<Look> {

    /** Catalogue search. Tags stay lazy here and are batch-fetched, so paging stays in SQL. */
    @Override
    @EntityGraph(attributePaths = {"brand", "author"})
    Page<Look> findAll(Specification<Look> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"brand", "author", "tags"})
    Optional<Look> findBySlug(String slug);

    boolean existsBySlug(String slug);

    long countByAuthorId(Long authorId);

    /**
     * Leaderboard for a time window: looks ranked by the mean score of the reviews written inside
     * that window, not by their all-time average. A minimum review count keeps a single 100 from
     * topping the chart.
     */
    @Query("""
            select r.look.id as lookId,
                   avg(r.score) as periodScore,
                   count(r.id) as periodReviews
            from Review r
            where r.createdAt >= :since
            group by r.look.id
            having count(r.id) >= :minReviews
            order by avg(r.score) desc, count(r.id) desc
            """)
    List<PeriodRank> rankByPeriod(@Param("since") Instant since,
                                  @Param("minReviews") long minReviews,
                                  Pageable pageable);

    /** Looks with the most review activity in a window — the "trending now" rail. */
    @Query("""
            select r.look.id as lookId,
                   avg(r.score) as periodScore,
                   count(r.id) as periodReviews
            from Review r
            where r.createdAt >= :since
            group by r.look.id
            order by count(r.id) desc, avg(r.score) desc
            """)
    List<PeriodRank> rankByActivity(@Param("since") Instant since, Pageable pageable);

    @EntityGraph(attributePaths = {"brand", "author", "tags"})
    List<Look> findByIdIn(Collection<Long> ids);

    /**
     * Content-based candidate pool for recommendations: unseen looks that share at least one tag
     * with the seed set, scored by tag overlap first and community rating second.
     */
    @Query("""
            select l.id as lookId, count(t.id) as overlap
            from Look l
            join l.tags t
            where t.id in :tagIds
              and l.id not in :excludedIds
              and l.ratingsCount > 0
            group by l.id
            order by count(t.id) desc, coalesce(l.avgScore, 0) desc
            """)
    List<TagOverlap> findByTagOverlap(@Param("tagIds") Collection<Long> tagIds,
                                      @Param("excludedIds") Collection<Long> excludedIds,
                                      Pageable pageable);

    /** Fallback pool when a user has not rated anything yet. */
    @EntityGraph(attributePaths = {"brand", "author"})
    @Query("""
            select l from Look l
            where l.ratingsCount >= :minRatings
            order by l.avgScore desc nulls last, l.ratingsCount desc
            """)
    List<Look> findHighestRated(@Param("minRatings") int minRatings, Pageable pageable);

    interface PeriodRank {
        Long getLookId();

        Double getPeriodScore();

        Long getPeriodReviews();
    }

    interface TagOverlap {
        Long getLookId();

        Long getOverlap();
    }
}
