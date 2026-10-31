package com.outfitrating.service;

import com.outfitrating.domain.Look;
import com.outfitrating.domain.Tag;
import com.outfitrating.dto.RecommendationDto;
import com.outfitrating.repo.LookRepository;
import com.outfitrating.repo.ReviewRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Content-based recommendations.
 *
 * <p>The taste profile is the set of tags on the looks a member scored at {@code LIKED_THRESHOLD}
 * or above. Candidates are unseen looks sharing those tags, ranked on tag overlap first and
 * community score second. Collaborative filtering would need a user base this project does not
 * have yet; tag overlap degrades gracefully to "the best-rated things you have not seen".
 */
@Service
public class RecommendationService {

    private static final int LIKED_THRESHOLD = 70;
    private static final int MIN_RATINGS_FOR_FALLBACK = 2;

    private final ReviewRepository reviews;
    private final LookRepository looks;
    private final LookService lookService;

    public RecommendationService(ReviewRepository reviews,
                                 LookRepository looks,
                                 LookService lookService) {
        this.reviews = reviews;
        this.looks = looks;
        this.lookService = lookService;
    }

    @Transactional(readOnly = true)
    public List<RecommendationDto> forUser(Long userId, int limit) {
        List<Long> alreadyRated = reviews.findRatedLookIds(userId);
        List<Long> preferredTags = reviews.findPreferredTagIds(userId, LIKED_THRESHOLD);

        if (preferredTags.isEmpty()) {
            return fallback(limit, alreadyRated);
        }

        Map<Long, Long> tagWeights = preferredTags.stream()
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));
        // "not in ()" is invalid SQL, so an empty exclusion list needs a harmless sentinel.
        List<Long> excluded = alreadyRated.isEmpty() ? List.of(-1L) : alreadyRated;

        List<LookRepository.TagOverlap> overlaps = looks.findByTagOverlap(
                List.copyOf(tagWeights.keySet()), excluded, PageRequest.of(0, limit * 2));
        if (overlaps.isEmpty()) {
            return fallback(limit, alreadyRated);
        }

        List<Look> candidates = lookService.orderedByIds(
                overlaps.stream().map(LookRepository.TagOverlap::getLookId).toList());
        Map<Long, Long> overlapById = overlaps.stream().collect(Collectors.toMap(
                LookRepository.TagOverlap::getLookId, LookRepository.TagOverlap::getOverlap));
        Map<Long, Boolean> favorited = lookService.favoriteFlags(candidates, userId);

        List<RecommendationDto> result = new ArrayList<>();
        for (Look look : candidates) {
            long overlap = overlapById.getOrDefault(look.getId(), 0L);
            double communityScore = look.getAvgScore() == null ? 0 : look.getAvgScore();
            double affinity = Math.min(100, overlap * 18 + communityScore * 0.55);
            result.add(new RecommendationDto(
                    lookService.toSummary(look, favorited.get(look.getId())),
                    reasonFor(look, overlap),
                    Math.round(affinity * 10) / 10.0));
        }
        result.sort((a, b) -> Double.compare(b.affinity(), a.affinity()));
        return result.stream().limit(limit).toList();
    }

    private List<RecommendationDto> fallback(int limit, List<Long> alreadyRated) {
        List<Look> top = looks.findHighestRated(MIN_RATINGS_FOR_FALLBACK,
                PageRequest.of(0, limit + alreadyRated.size()));
        return top.stream()
                .filter(look -> !alreadyRated.contains(look.getId()))
                .limit(limit)
                .map(look -> new RecommendationDto(
                        lookService.toSummary(look, null),
                        "Highly rated by the community — rate a few looks to personalise this",
                        look.getAvgScore() == null ? 0 : look.getAvgScore()))
                .toList();
    }

    private String reasonFor(Look look, long overlap) {
        String tagNames = look.getTags().stream()
                .map(Tag::getName)
                .limit(2)
                .collect(Collectors.joining(", "));
        if (overlap > 1 && !tagNames.isBlank()) {
            return "Matches " + overlap + " tags you rate highly, including " + tagNames;
        }
        if (!tagNames.isBlank()) {
            return "You have rated " + tagNames + " highly before";
        }
        return "Close to the " + look.getCategory().name().toLowerCase().replace('_', ' ')
                + " looks you score well";
    }
}
