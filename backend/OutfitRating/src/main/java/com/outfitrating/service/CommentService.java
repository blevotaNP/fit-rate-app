package com.outfitrating.service;

import com.outfitrating.domain.Comment;
import com.outfitrating.domain.Look;
import com.outfitrating.domain.User;
import com.outfitrating.dto.CommentDto;
import com.outfitrating.dto.CreateCommentRequest;
import com.outfitrating.exception.BadRequestException;
import com.outfitrating.exception.ForbiddenException;
import com.outfitrating.exception.NotFoundException;
import com.outfitrating.repo.CommentRepository;
import com.outfitrating.security.AppUserDetails;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Discussion under a look. Threads are deliberately one level deep — replies, not sub-forums. */
@Service
public class CommentService {

    private final CommentRepository comments;
    private final LookService lookService;
    private final DtoMapper mapper;

    public CommentService(CommentRepository comments, LookService lookService, DtoMapper mapper) {
        this.comments = comments;
        this.lookService = lookService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CommentDto> forLook(Long lookId, Long viewerId) {
        List<Comment> all = comments.findByLookIdOrderByCreatedAtAsc(lookId);

        Map<Long, List<CommentDto>> repliesByParent = new LinkedHashMap<>();
        for (Comment comment : all) {
            if (comment.getParent() != null) {
                repliesByParent
                        .computeIfAbsent(comment.getParent().getId(), key -> new ArrayList<>())
                        .add(mapper.toDto(comment, viewerId, List.of()));
            }
        }
        List<CommentDto> roots = new ArrayList<>();
        for (Comment comment : all) {
            if (comment.getParent() == null) {
                roots.add(mapper.toDto(comment, viewerId,
                        repliesByParent.getOrDefault(comment.getId(), List.of())));
            }
        }
        return roots;
    }

    @Transactional
    public CommentDto create(Long lookId, CreateCommentRequest request, User author) {
        Look look = lookService.requireById(lookId);

        Comment comment = new Comment();
        comment.setLook(look);
        comment.setAuthor(author);
        comment.setBody(request.body().trim());

        if (request.parentId() != null) {
            Comment parent = comments.findById(request.parentId())
                    .orElseThrow(() -> NotFoundException.of("Comment", request.parentId()));
            if (!parent.getLook().getId().equals(lookId)) {
                throw new BadRequestException("That comment belongs to a different look");
            }
            // Collapse deeper nesting back to the top-level thread.
            comment.setParent(parent.getParent() == null ? parent : parent.getParent());
        }

        comments.save(comment);
        return mapper.toDto(comment, author.getId(), List.of());
    }

    @Transactional
    public void delete(Long commentId, AppUserDetails viewer) {
        Comment comment = comments.findById(commentId)
                .orElseThrow(() -> NotFoundException.of("Comment", commentId));
        if (!viewer.isAdmin() && !viewer.id().equals(comment.getAuthor().getId())) {
            throw new ForbiddenException("Only the author or an admin can delete this comment");
        }
        comments.delete(comment);
    }
}
