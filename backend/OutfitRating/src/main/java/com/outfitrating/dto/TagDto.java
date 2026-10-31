package com.outfitrating.dto;

public record TagDto(Long id, String name, String slug, Long usageCount) {

    public static TagDto of(Long id, String name, String slug) {
        return new TagDto(id, name, slug, null);
    }
}
