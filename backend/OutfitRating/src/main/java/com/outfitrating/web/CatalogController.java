package com.outfitrating.web;

import com.outfitrating.domain.Category;
import com.outfitrating.domain.Occasion;
import com.outfitrating.domain.Season;
import com.outfitrating.dto.BrandDto;
import com.outfitrating.dto.PlatformStatsDto;
import com.outfitrating.dto.TagDto;
import com.outfitrating.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Reference data the client needs to render filters, plus the homepage counters. */
@Validated
@RestController
@RequestMapping("/api")
@Tag(name = "Catalogue")
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/brands")
    @Operation(summary = "Every brand in the catalogue")
    public List<BrandDto> brands() {
        return catalog.allBrands();
    }

    @GetMapping("/tags")
    @Operation(summary = "Tags ordered by how often they are used")
    public List<TagDto> tags(@RequestParam(defaultValue = "24") @Min(1) @Max(80) int limit) {
        return catalog.popularTags(limit);
    }

    @GetMapping("/stats")
    @Operation(summary = "Platform-wide counters")
    public PlatformStatsDto stats() {
        return catalog.stats();
    }

    @GetMapping("/taxonomy")
    @Operation(summary = "Enumerations the filter UI is built from")
    public Map<String, List<String>> taxonomy() {
        return Map.of(
                "categories", names(Category.values()),
                "seasons", names(Season.values()),
                "occasions", names(Occasion.values()));
    }

    private static List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }
}
