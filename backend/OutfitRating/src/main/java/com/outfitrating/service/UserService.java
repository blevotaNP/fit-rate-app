package com.outfitrating.service;

import com.outfitrating.domain.Follow;
import com.outfitrating.domain.User;
import com.outfitrating.dto.PageResponse;
import com.outfitrating.dto.UpdateProfileRequest;
import com.outfitrating.dto.UserProfileDto;
import com.outfitrating.dto.UserStatsDto;
import com.outfitrating.dto.UserSummaryDto;
import com.outfitrating.exception.BadRequestException;
import com.outfitrating.exception.ConflictException;
import com.outfitrating.exception.NotFoundException;
import com.outfitrating.repo.FavoriteRepository;
import com.outfitrating.repo.FollowRepository;
import com.outfitrating.repo.LookRepository;
import com.outfitrating.repo.ReviewLikeRepository;
import com.outfitrating.repo.ReviewRepository;
import com.outfitrating.repo.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;
    private final LookRepository looks;
    private final ReviewRepository reviews;
    private final ReviewLikeRepository reviewLikes;
    private final FavoriteRepository favorites;
    private final FollowRepository follows;
    private final DtoMapper mapper;

    public UserService(UserRepository users,
                       LookRepository looks,
                       ReviewRepository reviews,
                       ReviewLikeRepository reviewLikes,
                       FavoriteRepository favorites,
                       FollowRepository follows,
                       DtoMapper mapper) {
        this.users = users;
        this.looks = looks;
        this.reviews = reviews;
        this.reviewLikes = reviewLikes;
        this.favorites = favorites;
        this.follows = follows;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public User requireById(Long id) {
        return users.findById(id).orElseThrow(() -> NotFoundException.of("User", id));
    }

    @Transactional(readOnly = true)
    public User requireByUsername(String username) {
        return users.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new NotFoundException("No member called " + username));
    }

    @Transactional(readOnly = true)
    public UserProfileDto profileOf(String username, Long viewerId) {
        return toProfile(requireByUsername(username), viewerId);
    }

    @Transactional(readOnly = true)
    public UserProfileDto profileOf(Long userId) {
        return toProfile(requireById(userId), userId);
    }

    @Transactional
    public UserProfileDto updateProfile(Long userId, UpdateProfileRequest request) {
        User user = requireById(userId);
        if (request.displayName() != null && !request.displayName().isBlank()) {
            user.setDisplayName(request.displayName().trim());
        }
        if (request.bio() != null) {
            user.setBio(request.bio().isBlank() ? null : request.bio().trim());
        }
        if (request.location() != null) {
            user.setLocation(request.location().isBlank() ? null : request.location().trim());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl().isBlank() ? null : request.avatarUrl().trim());
        }
        return toProfile(user, userId);
    }

    @Transactional
    public void follow(Long followerId, String username) {
        User target = requireByUsername(username);
        if (target.getId().equals(followerId)) {
            throw new BadRequestException("You cannot follow yourself");
        }
        if (follows.existsByFollowerIdAndFollowingId(followerId, target.getId())) {
            throw new ConflictException("You already follow " + target.getUsername());
        }
        Follow follow = new Follow();
        follow.setFollower(requireById(followerId));
        follow.setFollowing(target);
        follows.save(follow);
    }

    @Transactional
    public void unfollow(Long followerId, String username) {
        User target = requireByUsername(username);
        follows.findByFollowerIdAndFollowingId(followerId, target.getId())
                .ifPresent(follows::delete);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserSummaryDto> followersOf(String username, Pageable pageable) {
        User user = requireByUsername(username);
        return PageResponse.from(
                follows.findByFollowingIdOrderByCreatedAtDesc(user.getId(), pageable),
                follow -> mapper.toSummary(follow.getFollower()));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserSummaryDto> followingOf(String username, Pageable pageable) {
        User user = requireByUsername(username);
        return PageResponse.from(
                follows.findByFollowerIdOrderByCreatedAtDesc(user.getId(), pageable),
                follow -> mapper.toSummary(follow.getFollowing()));
    }

    private UserProfileDto toProfile(User user, Long viewerId) {
        boolean self = viewerId != null && viewerId.equals(user.getId());
        Boolean followedByMe = viewerId == null || self
                ? null
                : follows.existsByFollowerIdAndFollowingId(viewerId, user.getId());
        return new UserProfileDto(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                self ? user.getEmail() : null,
                user.getAvatarUrl(),
                user.getBio(),
                user.getLocation(),
                user.getRole(),
                user.getCreatedAt(),
                statsFor(user),
                followedByMe);
    }

    private UserStatsDto statsFor(User user) {
        Long id = user.getId();
        long ratings = reviews.countByAuthorId(id);
        return new UserStatsDto(
                looks.countByAuthorId(id),
                ratings,
                reviews.countProseByAuthor(id),
                reviewLikes.countLikesReceivedBy(id),
                favorites.countByUserId(id),
                follows.countByFollowingId(id),
                follows.countByFollowerId(id),
                ratings == 0 ? 0 : Math.round(reviews.averageScoreGivenBy(id) * 10) / 10.0);
    }
}
