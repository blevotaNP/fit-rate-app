package com.outfitrating.web;

import com.outfitrating.domain.Category;
import com.outfitrating.domain.LookType;
import com.outfitrating.domain.Occasion;
import com.outfitrating.domain.Season;
import com.outfitrating.dto.CommentDto;
import com.outfitrating.dto.CreateCommentRequest;
import com.outfitrating.dto.CreateLookRequest;
import com.outfitrating.dto.LookDetailDto;
import com.outfitrating.dto.LookSummaryDto;
import com.outfitrating.dto.PageResponse;
import com.outfitrating.dto.ReviewDto;
import com.outfitrating.dto.SaveReviewRequest;
import com.outfitrating.security.AppUserDetails;
import com.outfitrating.service.CommentService;
import com.outfitrating.service.FavoriteService;
import com.outfitrating.service.LookQuery;
import com.outfitrating.service.LookService;
import com.outfitrating.service.ReviewService;
import com.outfitrating.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The catalogue: browsing, publishing, rating, saving and discussing looks. */
@Validated
@RestController
@RequestMapping("/api/looks")
@Tag(name = "Looks")
public class LookController {

    private final LookService lookService;
    private final ReviewService reviewService;
    private final CommentService commentService;
    private final FavoriteService favoriteService;
    private final UserService userService;

    public LookController(LookService lookService,
                          ReviewService reviewService,
                          CommentService commentService,
                          FavoriteService favoriteService,
                          UserService userService) {
        this.lookService = lookService;
        this.reviewService = reviewService;
        this.commentService = commentService;
        this.favoriteService = favoriteService;
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Browse and filter the catalogue")
    public PageResponse<LookSummaryDto> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) LookType type,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Season season,
            @RequestParam(required = false) Occasion occasion,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) @Min(0) @Max(100) Integer minScore,
            @RequestParam(required = false) LookQuery.SortOrder sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "24") @Min(1) @Max(60) int size,
            @AuthenticationPrincipal AppUserDetails principal) {
        LookQuery query = new LookQuery(q, type, category, season, occasion, brand, tag, author,
                featured, minScore, sort);
        return lookService.search(query, page, size, idOf(principal));
    }

    @GetMapping("/featured")
    @Operation(summary = "Editor picks for the homepage hero")
    public List<LookSummaryDto> featured(@RequestParam(defaultValue = "6") @Min(1) @Max(24) int limit,
                                         @AuthenticationPrincipal AppUserDetails principal) {
        return lookService.featured(limit, idOf(principal));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "One look with its full score breakdown")
    public LookDetailDto detail(@PathVariable String slug,
                                @AuthenticationPrincipal AppUserDetails principal) {
        return lookService.detailBySlug(slug, principal);
    }

    @GetMapping("/{slug}/similar")
    @Operation(summary = "Looks in the same vein")
    public List<LookSummaryDto> similar(@PathVariable String slug,
                                        @RequestParam(defaultValue = "6") @Min(1) @Max(24) int limit,
                                        @AuthenticationPrincipal AppUserDetails principal) {
        return lookService.similarTo(slug, limit, idOf(principal));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Publish an outfit or a garment")
    public LookDetailDto create(@Valid @RequestBody CreateLookRequest request,
                                @AuthenticationPrincipal AppUserDetails principal) {
        return lookService.create(request, userService.requireById(principal.id()), principal);
    }

    @PutMapping("/{slug}")
    @Operation(summary = "Edit a look you published")
    public LookDetailDto update(@PathVariable String slug,
                                @Valid @RequestBody CreateLookRequest request,
                                @AuthenticationPrincipal AppUserDetails principal) {
        return lookService.update(slug, request, principal);
    }

    @DeleteMapping("/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Withdraw a look you published")
    public void delete(@PathVariable String slug,
                       @AuthenticationPrincipal AppUserDetails principal) {
        lookService.delete(slug, principal);
    }

    // --- reviews ---------------------------------------------------------------

    @GetMapping("/{slug}/reviews")
    @Operation(summary = "Verdicts on this look")
    public PageResponse<ReviewDto> reviews(@PathVariable String slug,
                                           @RequestParam(defaultValue = "newest") String sort,
                                           @RequestParam(defaultValue = "0") @Min(0) int page,
                                           @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
                                           @AuthenticationPrincipal AppUserDetails principal) {
        Long lookId = lookService.requireBySlug(slug).getId();
        return reviewService.forLook(lookId, page, size, sort, idOf(principal));
    }

    @PutMapping("/{slug}/review")
    @Operation(summary = "Score this look, replacing your previous verdict if any")
    public ReviewDto saveReview(@PathVariable String slug,
                                @Valid @RequestBody SaveReviewRequest request,
                                @AuthenticationPrincipal AppUserDetails principal) {
        Long lookId = lookService.requireBySlug(slug).getId();
        return reviewService.save(lookId, request, userService.requireById(principal.id()));
    }

    // --- wardrobe --------------------------------------------------------------

    @PostMapping("/{slug}/favorite")
    @Operation(summary = "Save or unsave this look")
    public Map<String, Boolean> toggleFavorite(@PathVariable String slug,
                                               @AuthenticationPrincipal AppUserDetails principal) {
        Long lookId = lookService.requireBySlug(slug).getId();
        boolean saved = favoriteService.toggle(lookId, userService.requireById(principal.id()));
        return Map.of("favorited", saved);
    }

    // --- discussion ------------------------------------------------------------

    @GetMapping("/{slug}/comments")
    @Operation(summary = "Discussion thread")
    public List<CommentDto> comments(@PathVariable String slug,
                                     @AuthenticationPrincipal AppUserDetails principal) {
        Long lookId = lookService.requireBySlug(slug).getId();
        return commentService.forLook(lookId, idOf(principal));
    }

    @PostMapping("/{slug}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Post a comment or a reply")
    public CommentDto comment(@PathVariable String slug,
                              @Valid @RequestBody CreateCommentRequest request,
                              @AuthenticationPrincipal AppUserDetails principal) {
        Long lookId = lookService.requireBySlug(slug).getId();
        return commentService.create(lookId, request, userService.requireById(principal.id()));
    }

    private static Long idOf(AppUserDetails principal) {
        return principal == null ? null : principal.id();
    }
}
