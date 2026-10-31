package com.outfitrating.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "brands")
public class Brand extends BaseEntity {

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Column(nullable = false, unique = true, length = 96)
    private String slug;

    @Column(length = 64)
    private String country;

    @Column(name = "logo_url", length = 512)
    private String logoUrl;

    @Column(length = 600)
    private String description;
}
