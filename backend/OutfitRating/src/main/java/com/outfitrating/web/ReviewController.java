package com.outfitrating.web;

import com.outfitrating.dto.PageResponse;
import com.outfitrating.dto.ReviewDto;
import com.outfitrating.security.AppUserDetails;
import com.outfitrating.service.CommentService;
import com.outfitrating.service.ReviewService;
import com.outfitrating.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api")
@Tag(name = "Reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final CommentService commentService;
    private final UserService userService;

    public ReviewController(ReviewService reviewService,
                            CommentService commentService,
                            UserService userService) {
        this.reviewService = reviewService;
        this.commentService = commentService;
        this.userService = userService;
    }

    @GetMapping("/reviews/feed")
    @Operation(summary = "Latest written reviews across the platform")
    public PageResponse<ReviewDto> feed(@RequestParam(defaultValue = "0") @Min(0) int page,
                                        @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
                                        @AuthenticationPrincipal AppUserDetails principal) {
        return reviewService.feed(page, size, principal == null ? null : principal.id());
    }

    @PostMapping("/reviews/{id}/like")
    @Operation(summary = "Toggle a helpful-vote on a review")
    public Map<String, Integer> like(@PathVariable Long id,
                                     @AuthenticationPrincipal AppUserDetails principal) {
        int likes = reviewService.toggleLike(id, userService.requireById(principal.id()));
        return Map.of("likesCount", likes);
    }

    @DeleteMapping("/reviews/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a review you wrote")
    public void deleteReview(@PathVariable Long id,
                             @AuthenticationPrincipal AppUserDetails principal) {
        reviewService.delete(id, principal);
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a comment you wrote")
    public void deleteComment(@PathVariable Long id,
                              @AuthenticationPrincipal AppUserDetails principal) {
        commentService.delete(id, principal);
    }
}
