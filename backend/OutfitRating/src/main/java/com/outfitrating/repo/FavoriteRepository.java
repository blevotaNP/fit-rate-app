package com.outfitrating.repo;

import com.outfitrating.domain.Favorite;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserIdAndLookId(Long userId, Long lookId);

    boolean existsByUserIdAndLookId(Long userId, Long lookId);

    @EntityGraph(attributePaths = {"look", "look.brand", "look.author"})
    Page<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserId(Long userId);

    long countByLookId(Long lookId);

    @Query("select f.look.id from Favorite f where f.user.id = :userId and f.look.id in :lookIds")
    List<Long> findFavoritedLookIds(@Param("userId") Long userId,
                                    @Param("lookIds") Collection<Long> lookIds);
}
