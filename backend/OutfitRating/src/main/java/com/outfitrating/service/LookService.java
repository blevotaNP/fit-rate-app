package com.outfitrating.service;

import com.outfitrating.domain.Brand;
import com.outfitrating.domain.Look;
import com.outfitrating.domain.Review;
import com.outfitrating.domain.Tag;
import com.outfitrating.domain.User;
import com.outfitrating.dto.CreateLookRequest;
import com.outfitrating.dto.LookDetailDto;
import com.outfitrating.dto.LookSummaryDto;
import com.outfitrating.dto.PageResponse;
import com.outfitrating.exception.ForbiddenException;
import com.outfitrating.exception.NotFoundException;
import com.outfitrating.repo.BrandRepository;
import com.outfitrating.repo.CommentRepository;
import com.outfitrating.repo.FavoriteRepository;
import com.outfitrating.repo.LookRepository;
import com.outfitrating.repo.ReviewRepository;
import com.outfitrating.repo.TagRepository;
import com.outfitrating.security.AppUserDetails;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LookService {

    private final LookRepository looks;
    private final BrandRepository brands;
    private final TagRepository tags;
    private final ReviewRepository reviews;
    private final CommentRepository comments;
    private final FavoriteRepository favorites;
    private final SlugGenerator slugs;
    private final DtoMapper mapper;

    public LookService(LookRepository looks,
                       BrandRepository brands,
                       TagRepository tags,
                       ReviewRepository reviews,
                       CommentRepository comments,
                       FavoriteRepository favorites,
                       SlugGenerator slugs,
                       DtoMapper mapper) {
        this.looks = looks;
        this.brands = brands;
        this.tags = tags;
        this.reviews = reviews;
        this.comments = comments;
        this.favorites = favorites;
        this.slugs = slugs;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<LookSummaryDto> search(LookQuery query, int page, int size, Long viewerId) {
        List<Specification<Look>> filters = new ArrayList<>(List.of());
        addIfPresent(filters, LookSpecifications.matches(query.q()));
        addIfPresent(filters, LookSpecifications.typeIs(query.type()));
        addIfPresent(filters, LookSpecifications.categoryIs(query.category()));
        addIfPresent(filters, LookSpecifications.seasonIs(query.season()));
        addIfPresent(filters, LookSpecifications.occasionIs(query.occasion()));
        addIfPresent(filters, LookSpecifications.brandSlugIs(query.brand()));
        addIfPresent(filters, LookSpecifications.hasTag(query.tag()));
        addIfPresent(filters, LookSpecifications.authorIs(query.author()));
        addIfPresent(filters, LookSpecifications.featuredOnly(query.featured()));
        addIfPresent(filters, LookSpecifications.scoredAtLeast(query.minScore()));

        Pageable pageable = PageRequest.of(page, size, query.sortOrDefault().sort());
        Page<Look> result = looks.findAll(Specification.allOf(filters), pageable);
        Map<Long, Boolean> favorited = favoriteFlags(result.getContent(), viewerId);
        return PageResponse.from(result, look -> mapper.toSummary(look, favorited.get(look.getId())));
    }

    @Transactional(readOnly = true)
    public LookDetailDto detailBySlug(String slug, AppUserDetails viewer) {
        Look look = looks.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("No look at /" + slug));
        Long viewerId = viewer == null ? null : viewer.id();

        Review myReview = viewerId == null
                ? null
                : reviews.findByLookIdAndAuthorId(look.getId(), viewerId).orElse(null);

        return new LookDetailDto(
                look.getId(),
                look.getSlug(),
                look.getTitle(),
                look.getType(),
                look.getCategory(),
                look.getSeason(),
                look.getOccasion(),
                look.getDescription(),
                look.getImageUrl(),
                mapper.toDto(look.getBrand()),
                mapper.toSummary(look.getAuthor()),
                look.getPriceCents(),
                look.getCurrency(),
                look.getReleaseDate(),
                look.isFeatured(),
                look.getRatingsCount(),
                look.getReviewsCount(),
                comments.countByLookId(look.getId()),
                favorites.countByLookId(look.getId()),
                mapper.toBreakdown(look),
                mapper.toTagDtos(look),
                look.getCreatedAt(),
                viewerId == null ? null : favorites.existsByUserIdAndLookId(viewerId, look.getId()),
                myReview == null ? null : mapper.toDto(myReview, null, viewerId, false),
                canEdit(look, viewer));
    }

    @Transactional(readOnly = true)
    public Look requireBySlug(String slug) {
        return looks.findBySlug(slug).orElseThrow(() -> new NotFoundException("No look at /" + slug));
    }

    @Transactional(readOnly = true)
    public Look requireById(Long id) {
        return looks.findById(id).orElseThrow(() -> NotFoundException.of("Look", id));
    }

    @Transactional
    public LookDetailDto create(CreateLookRequest request, User author, AppUserDetails viewer) {
        Look look = new Look();
        look.setSlug(slugs.uniqueSlug(request.title(), looks::existsBySlug));
        applyRequest(look, request);
        look.setAuthor(author);
        looks.save(look);
        return detailBySlug(look.getSlug(), viewer);
    }

    @Transactional
    public LookDetailDto update(String slug, CreateLookRequest request, AppUserDetails viewer) {
        Look look = requireBySlug(slug);
        requireEditable(look, viewer);
        applyRequest(look, request);
        return detailBySlug(look.getSlug(), viewer);
    }

    @Transactional
    public void delete(String slug, AppUserDetails viewer) {
        Look look = requireBySlug(slug);
        requireEditable(look, viewer);
        looks.delete(look);
    }

    /** Same category or shared tags — the "you might also like" strip on a look page. */
    @Transactional(readOnly = true)
    public List<LookSummaryDto> similarTo(String slug, int limit, Long viewerId) {
        Look look = requireBySlug(slug);
        List<Long> tagIds = look.getTags().stream().map(Tag::getId).toList();

        List<Look> candidates = new ArrayList<>();
        if (!tagIds.isEmpty()) {
            List<Long> ids = looks.findByTagOverlap(tagIds, List.of(look.getId()),
                            PageRequest.of(0, limit))
                    .stream().map(LookRepository.TagOverlap::getLookId).toList();
            candidates.addAll(orderedByIds(ids));
        }
        if (candidates.size() < limit) {
            LookQuery fill = new LookQuery(null, look.getType(), look.getCategory(), null, null,
                    null, null, null, null, null, LookQuery.SortOrder.TOP_RATED);
            searchEntities(fill, limit + 1).stream()
                    .filter(candidate -> !candidate.getId().equals(look.getId()))
                    .filter(candidate -> candidates.stream()
                            .noneMatch(existing -> existing.getId().equals(candidate.getId())))
                    .limit(limit - candidates.size())
                    .forEach(candidates::add);
        }
        Map<Long, Boolean> favorited = favoriteFlags(candidates, viewerId);
        return candidates.stream()
                .limit(limit)
                .map(candidate -> mapper.toSummary(candidate, favorited.get(candidate.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LookSummaryDto> featured(int limit, Long viewerId) {
        LookQuery query = new LookQuery(null, null, null, null, null, null, null, null,
                true, null, LookQuery.SortOrder.TOP_RATED);
        List<Look> result = searchEntities(query, limit);
        Map<Long, Boolean> favorited = favoriteFlags(result, viewerId);
        return result.stream().map(look -> mapper.toSummary(look, favorited.get(look.getId()))).toList();
    }

    /** Resolves ids to entities while preserving the ranking order the caller computed. */
    @Transactional(readOnly = true)
    public List<Look> orderedByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Look> byId = looks.findByIdIn(ids).stream()
                .collect(Collectors.toMap(Look::getId, Function.identity()));
        return ids.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, Boolean> favoriteFlags(List<Look> page, Long viewerId) {
        if (viewerId == null || page.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = page.stream().map(Look::getId).toList();
        Set<Long> favorited = Set.copyOf(favorites.findFavoritedLookIds(viewerId, ids));
        return ids.stream().collect(Collectors.toMap(Function.identity(), favorited::contains));
    }

    public LookSummaryDto toSummary(Look look, Boolean favorited) {
        return mapper.toSummary(look, favorited);
    }

    private List<Look> searchEntities(LookQuery query, int limit) {
        List<Specification<Look>> filters = new ArrayList<>();
        addIfPresent(filters, LookSpecifications.typeIs(query.type()));
        addIfPresent(filters, LookSpecifications.categoryIs(query.category()));
        addIfPresent(filters, LookSpecifications.featuredOnly(query.featured()));
        return looks.findAll(Specification.allOf(filters),
                PageRequest.of(0, limit, query.sortOrDefault().sort())).getContent();
    }

    private void applyRequest(Look look, CreateLookRequest request) {
        look.setTitle(request.title().trim());
        look.setType(request.type());
        look.setCategory(request.category());
        look.setSeason(request.season());
        look.setOccasion(request.occasion());
        look.setDescription(blankToNull(request.description()));
        look.setImageUrl(request.imageUrl().trim());
        look.setPriceCents(request.priceCents());
        look.setCurrency(request.currency() == null
                ? null
                : request.currency().toUpperCase(Locale.ROOT));
        look.setReleaseDate(request.releaseDate());
        look.setBrand(resolveBrand(request.brandName()));
        look.setTags(resolveTags(request.tags()));
    }

    private Brand resolveBrand(String brandName) {
        if (brandName == null || brandName.isBlank()) {
            return null;
        }
        String name = brandName.trim();
        return brands.findByNameIgnoreCase(name).orElseGet(() -> {
            Brand brand = new Brand();
            brand.setName(name);
            brand.setSlug(slugs.uniqueSlug(name, slug -> brands.findBySlug(slug).isPresent()));
            return brands.save(brand);
        });
    }

    private Set<Tag> resolveTags(List<String> names) {
        Set<Tag> resolved = new LinkedHashSet<>();
        if (names == null) {
            return resolved;
        }
        for (String raw : names) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String name = raw.trim().toLowerCase(Locale.ROOT);
            resolved.add(tags.findByNameIgnoreCase(name).orElseGet(() -> {
                Tag tag = new Tag();
                tag.setName(name);
                tag.setSlug(slugs.uniqueSlug(name, slug -> tags.findBySlug(slug).isPresent()));
                return tags.save(tag);
            }));
        }
        return resolved;
    }

    private void requireEditable(Look look, AppUserDetails viewer) {
        if (!canEdit(look, viewer)) {
            throw new ForbiddenException("Only the author or an admin can change this look");
        }
    }

    private boolean canEdit(Look look, AppUserDetails viewer) {
        return viewer != null
                && (viewer.isAdmin() || viewer.id().equals(look.getAuthor().getId()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void addIfPresent(List<Specification<Look>> target, Specification<Look> spec) {
        if (spec != null) {
            target.add(spec);
        }
    }
}
