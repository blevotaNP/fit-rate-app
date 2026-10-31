package com.outfitrating.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * One verdict on one look by one user: five criteria scored 1..10 plus optional prose.
 * A row with a blank body is a bare rating; a row with prose is a full review.
 */
@Getter
@Setter
@Entity
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(
        name = "uq_reviews_look_author", columnNames = {"look_id", "author_id"}))
public class Review extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "look_id", nullable = false)
    private Look look;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false)
    private short style;

    @Column(nullable = false)
    private short fit;

    @Column(nullable = false)
    private short color;

    @Column(nullable = false)
    private short originality;

    @Column(nullable = false)
    private short quality;

    /** Composite verdict on a 0..100 scale, derived from the five criteria. */
    @Column(nullable = false)
    private short score;

    @Column(length = 140)
    private String title;

    @Column(length = 5000)
    private String body;

    @Column(name = "likes_count", nullable = false)
    private int likesCount = 0;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean hasProse() {
        return body != null && !body.isBlank();
    }
}
