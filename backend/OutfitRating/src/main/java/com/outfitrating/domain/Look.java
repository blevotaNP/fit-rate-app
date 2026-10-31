package com.outfitrating.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * A rateable entry: either a complete outfit or a single garment.
 *
 * <p>The aggregate columns (ratingsCount, reviewsCount, avgScore and the five per-criterion
 * averages) are denormalised snapshots recomputed by {@code ScoreAggregationService} whenever a
 * review is written, so catalogue queries and leaderboards never have to group over the whole
 * review table.
 */
@Getter
@Setter
@Entity
@Table(name = "looks")
public class Look extends BaseEntity {

    @Column(nullable = false, unique = true, length = 160)
    private String slug;

    @Column(nullable = false, length = 120)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LookType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Season season;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private Occasion occasion;

    @Column(length = 2000)
    private String description;

    @Column(name = "image_url", nullable = false, length = 512)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "price_cents")
    private Integer priceCents;

    @Column(length = 3)
    private String currency;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(nullable = false)
    private boolean featured = false;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "look_tags",
            joinColumns = @JoinColumn(name = "look_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new LinkedHashSet<>();

    // --- denormalised aggregates ------------------------------------------------

    /** Number of review rows; every review carries a rating. */
    @Column(name = "ratings_count", nullable = false)
    private int ratingsCount = 0;

    /** Subset of ratings that also carry written prose. */
    @Column(name = "reviews_count", nullable = false)
    private int reviewsCount = 0;

    @Column(name = "avg_score")
    private Double avgScore;

    @Column(name = "avg_style")
    private Double avgStyle;

    @Column(name = "avg_fit")
    private Double avgFit;

    @Column(name = "avg_color")
    private Double avgColor;

    @Column(name = "avg_originality")
    private Double avgOriginality;

    @Column(name = "avg_quality")
    private Double avgQuality;
}
