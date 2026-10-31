package com.outfitrating.service;

import com.outfitrating.domain.Favorite;
import com.outfitrating.domain.Look;
import com.outfitrating.domain.User;
import com.outfitrating.dto.LookSummaryDto;
import com.outfitrating.dto.PageResponse;
import com.outfitrating.repo.FavoriteRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The wardrobe: looks a member saved for later. */
@Service
public class FavoriteService {

    private final FavoriteRepository favorites;
    private final LookService lookService;

    public FavoriteService(FavoriteRepository favorites, LookService lookService) {
        this.favorites = favorites;
        this.lookService = lookService;
    }

    /** Idempotent toggle; returns whether the look is saved after the call. */
    @Transactional
    public boolean toggle(Long lookId, User user) {
        Look look = lookService.requireById(lookId);
        var existing = favorites.findByUserIdAndLookId(user.getId(), lookId);
        if (existing.isPresent()) {
            favorites.delete(existing.get());
            return false;
        }
        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setLook(look);
        favorites.save(favorite);
        return true;
    }

    @Transactional(readOnly = true)
    public PageResponse<LookSummaryDto> wardrobeOf(Long userId, int page, int size, Long viewerId) {
        var result = favorites.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        var flags = lookService.favoriteFlags(
                result.getContent().stream().map(Favorite::getLook).toList(), viewerId);
        return PageResponse.from(result,
                favorite -> lookService.toSummary(favorite.getLook(),
                        flags.get(favorite.getLook().getId())));
    }
}
