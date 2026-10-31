package com.outfitrating.repo;

import com.outfitrating.domain.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"author", "parent"})
    List<Comment> findByLookIdOrderByCreatedAtAsc(Long lookId);

    long countByLookId(Long lookId);
}
