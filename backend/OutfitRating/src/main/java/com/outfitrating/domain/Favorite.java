package com.outfitrating.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/** A look saved into the wardrobe of a user. */
@Getter
@Setter
@Entity
@Table(name = "favorites", uniqueConstraints = @UniqueConstraint(
        name = "uq_favorites_user_look", columnNames = {"user_id", "look_id"}))
public class Favorite extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "look_id", nullable = false)
    private Look look;
}
