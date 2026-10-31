package com.outfitrating.web;

import com.outfitrating.dto.LookSummaryDto;
import com.outfitrating.dto.PageResponse;
import com.outfitrating.dto.ReviewDto;
import com.outfitrating.dto.UserProfileDto;
import com.outfitrating.dto.UserSummaryDto;
import com.outfitrating.security.AppUserDetails;
import com.outfitrating.service.FavoriteService;
import com.outfitrating.service.LookQuery;
import com.outfitrating.service.LookService;
import com.outfitrating.service.ReviewService;
import com.outfitrating.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
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
@RequestMapping("/api/users")
@Tag(name = "Members")
public class UserController {

    private final UserService userService;
    private final LookService lookService;
    private final ReviewService reviewService;
    private final FavoriteService favoriteService;

    public UserController(UserService userService,
                          LookService lookService,
                          ReviewService reviewService,
                          FavoriteService favoriteService) {
        this.userService = userService;
        this.lookService = lookService;
        this.reviewService = reviewService;
        this.favoriteService = favoriteService;
    }

    @GetMapping("/{username}")
    @Operation(summary = "Public profile with activity stats")
    public UserProfileDto profile(@PathVariable String username,
                                  @AuthenticationPrincipal AppUserDetails principal) {
        return userService.profileOf(username, idOf(principal));
    }

    @GetMapping("/{username}/looks")
    @Operation(summary = "Looks this member published")
    public PageResponse<LookSummaryDto> looks(@PathVariable String username,
                                              @RequestParam(defaultValue = "0") @Min(0) int page,
                                              @RequestParam(defaultValue = "12") @Min(1) @Max(48) int size,
                                              @AuthenticationPrincipal AppUserDetails principal) {
        LookQuery query = new LookQuery(null, null, null, null, null, null, null, username,
                null, null, LookQuery.SortOrder.NEWEST);
        return lookService.search(query, page, size, idOf(principal));
    }

    @GetMapping("/{username}/reviews")
    @Operation(summary = "Verdicts this member has written")
    public PageResponse<ReviewDto> reviews(@PathVariable String username,
                                           @RequestParam(defaultValue = "0") @Min(0) int page,
                                           @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
                                           @AuthenticationPrincipal AppUserDetails principal) {
        Long authorId = userService.requireByUsername(username).getId();
        return reviewService.byAuthor(authorId, page, size, idOf(principal));
    }

    @GetMapping("/{username}/wardrobe")
    @Operation(summary = "Looks this member saved")
    public PageResponse<LookSummaryDto> wardrobe(@PathVariable String username,
                                                 @RequestParam(defaultValue = "0") @Min(0) int page,
                                                 @RequestParam(defaultValue = "12") @Min(1) @Max(48) int size,
                                                 @AuthenticationPrincipal AppUserDetails principal) {
        Long userId = userService.requireByUsername(username).getId();
        return favoriteService.wardrobeOf(userId, page, size, idOf(principal));
    }

    @GetMapping("/{username}/followers")
    @Operation(summary = "Members following this profile")
    public PageResponse<UserSummaryDto> followers(@PathVariable String username,
                                                  @RequestParam(defaultValue = "0") @Min(0) int page,
                                                  @RequestParam(defaultValue = "24") @Min(1) @Max(60) int size) {
        return userService.followersOf(username, PageRequest.of(page, size));
    }

    @GetMapping("/{username}/following")
    @Operation(summary = "Members this profile follows")
    public PageResponse<UserSummaryDto> following(@PathVariable String username,
                                                  @RequestParam(defaultValue = "0") @Min(0) int page,
                                                  @RequestParam(defaultValue = "24") @Min(1) @Max(60) int size) {
        return userService.followingOf(username, PageRequest.of(page, size));
    }

    @PostMapping("/{username}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Follow a member")
    public void follow(@PathVariable String username,
                       @AuthenticationPrincipal AppUserDetails principal) {
        userService.follow(principal.id(), username);
    }

    @DeleteMapping("/{username}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Unfollow a member")
    public void unfollow(@PathVariable String username,
                         @AuthenticationPrincipal AppUserDetails principal) {
        userService.unfollow(principal.id(), username);
    }

    private static Long idOf(AppUserDetails principal) {
        return principal == null ? null : principal.id();
    }
}
