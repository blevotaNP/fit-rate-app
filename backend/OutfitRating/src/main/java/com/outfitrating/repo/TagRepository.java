package com.outfitrating.repo;

import com.outfitrating.domain.Tag;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findBySlug(String slug);

    Optional<Tag> findByNameIgnoreCase(String name);

    /** Tags ordered by how many looks carry them — powers the "trending tags" rail. */
    @Query("""
            select t as tag, count(l.id) as usageCount
            from Look l
            join l.tags t
            group by t
            order by count(l.id) desc, t.name asc
            """)
    List<TagUsage> findMostUsed(Pageable pageable);

    interface TagUsage {
        Tag getTag();

        long getUsageCount();
    }
}
