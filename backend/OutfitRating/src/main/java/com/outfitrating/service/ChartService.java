package com.outfitrating.service;

import com.outfitrating.domain.Look;
import com.outfitrating.dto.ChartEntryDto;
import com.outfitrating.dto.LookSummaryDto;
import com.outfitrating.repo.LookRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Leaderboards.
 *
 * <p>A period chart ranks by the mean of the reviews written <em>inside</em> that window rather
 * than the all-time average, so a look that peaked six months ago does not squat on the weekly
 * chart forever. The minimum-review threshold scales with the window: a single enthusiastic 100
 * should not outrank a look ten people agreed on.
 */
@Service
public class ChartService {

    public enum Period {
        WEEK(7, 2),
        MONTH(30, 3),
        YEAR(365, 5),
        ALL_TIME(3650, 3);

        private final int days;
        private final int minReviews;

        Period(int days, int minReviews) {
            this.days = days;
            this.minReviews = minReviews;
        }

        public Instant since() {
            return Instant.now().minus(days, ChronoUnit.DAYS);
        }

        public int minReviews() {
            return minReviews;
        }
    }

    private final LookRepository looks;
    private final LookService lookService;

    public ChartService(LookRepository looks, LookService lookService) {
        this.looks = looks;
        this.lookService = lookService;
    }

    @Transactional(readOnly = true)
    public List<ChartEntryDto> chart(Period period, int limit, Long viewerId) {
        var ranked = looks.rankByPeriod(period.since(), period.minReviews(),
                PageRequest.of(0, limit));
        return assemble(ranked, viewerId);
    }

    /** Most-discussed looks of the last 48 hours — the "right now" rail on the homepage. */
    @Transactional(readOnly = true)
    public List<ChartEntryDto> trending(int limit, Long viewerId) {
        var ranked = looks.rankByActivity(Instant.now().minus(2, ChronoUnit.DAYS),
                PageRequest.of(0, limit));
        if (ranked.isEmpty()) {
            ranked = looks.rankByActivity(Instant.now().minus(30, ChronoUnit.DAYS),
                    PageRequest.of(0, limit));
        }
        return assemble(ranked, viewerId);
    }

    private List<ChartEntryDto> assemble(List<LookRepository.PeriodRank> ranked, Long viewerId) {
        if (ranked.isEmpty()) {
            return List.of();
        }
        Map<Long, LookRepository.PeriodRank> metrics = new LinkedHashMap<>();
        ranked.forEach(rank -> metrics.put(rank.getLookId(), rank));

        List<Look> ordered = lookService.orderedByIds(List.copyOf(metrics.keySet()));
        Map<Long, Boolean> favorited = lookService.favoriteFlags(ordered, viewerId);

        List<ChartEntryDto> entries = new ArrayList<>(ordered.size());
        int rank = 1;
        for (Look look : ordered) {
            LookRepository.PeriodRank metric = metrics.get(look.getId());
            LookSummaryDto summary = lookService.toSummary(look, favorited.get(look.getId()));
            entries.add(new ChartEntryDto(
                    rank++,
                    summary,
                    Math.round(metric.getPeriodScore() * 10) / 10.0,
                    metric.getPeriodReviews()));
        }
        return entries;
    }
}
