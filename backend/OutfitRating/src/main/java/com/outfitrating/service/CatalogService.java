package com.outfitrating.service;

import com.outfitrating.domain.LookType;
import com.outfitrating.dto.BrandDto;
import com.outfitrating.dto.PlatformStatsDto;
import com.outfitrating.dto.TagDto;
import com.outfitrating.repo.BrandRepository;
import com.outfitrating.repo.LookRepository;
import com.outfitrating.repo.ReviewRepository;
import com.outfitrating.repo.TagRepository;
import com.outfitrating.repo.UserRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reference data (brands, tags) and the headline counters shown on the homepage. */
@Service
public class CatalogService {

    private final BrandRepository brands;
    private final TagRepository tags;
    private final LookRepository looks;
    private final ReviewRepository reviews;
    private final UserRepository users;
    private final DtoMapper mapper;

    public CatalogService(BrandRepository brands,
                          TagRepository tags,
                          LookRepository looks,
                          ReviewRepository reviews,
                          UserRepository users,
                          DtoMapper mapper) {
        this.brands = brands;
        this.tags = tags;
        this.looks = looks;
        this.reviews = reviews;
        this.users = users;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<BrandDto> allBrands() {
        return brands.findAllByOrderByNameAsc().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<TagDto> popularTags(int limit) {
        return tags.findMostUsed(PageRequest.of(0, limit)).stream()
                .map(usage -> new TagDto(
                        usage.getTag().getId(),
                        usage.getTag().getName(),
                        usage.getTag().getSlug(),
                        usage.getUsageCount()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PlatformStatsDto stats() {
        long outfits = looks.count(Specification.allOf(
                List.of(LookSpecifications.typeIs(LookType.OUTFIT))));
        long items = looks.count(Specification.allOf(
                List.of(LookSpecifications.typeIs(LookType.ITEM))));
        long ratings = reviews.count();
        return new PlatformStatsDto(
                outfits + items,
                outfits,
                items,
                ratings,
                proseReviewCount(),
                users.count(),
                brands.count());
    }

    private long proseReviewCount() {
        return reviews.findProseFeed(PageRequest.of(0, 1)).getTotalElements();
    }
}
