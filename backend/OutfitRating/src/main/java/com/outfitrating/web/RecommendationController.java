package com.outfitrating.web;

import com.outfitrating.dto.RecommendationDto;
import com.outfitrating.security.AppUserDetails;
import com.outfitrating.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/recommendations")
@Tag(name = "Recommendations")
public class RecommendationController {

    private final RecommendationService recommendations;

    public RecommendationController(RecommendationService recommendations) {
        this.recommendations = recommendations;
    }

    @GetMapping
    @Operation(summary = "Looks picked for the signed-in member, with the reason for each pick")
    public List<RecommendationDto> forMe(
            @RequestParam(defaultValue = "12") @Min(1) @Max(30) int limit,
            @AuthenticationPrincipal AppUserDetails principal) {
        return recommendations.forUser(principal.id(), limit);
    }
}
