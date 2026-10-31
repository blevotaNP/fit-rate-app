package com.outfitrating.dto;

public record BrandDto(
        Long id,
        String name,
        String slug,
        String country,
        String logoUrl,
        String description) {
}
