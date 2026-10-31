package com.outfitrating.web;

import com.outfitrating.dto.ChartEntryDto;
import com.outfitrating.security.AppUserDetails;
import com.outfitrating.service.ChartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/charts")
@Tag(name = "Leaderboards")
public class ChartController {

    private final ChartService chartService;

    public ChartController(ChartService chartService) {
        this.chartService = chartService;
    }

    @GetMapping("/{period}")
    @Operation(summary = "Best-rated looks of a period (week, month, year, all_time)")
    public List<ChartEntryDto> chart(@PathVariable ChartService.Period period,
                                     @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit,
                                     @AuthenticationPrincipal AppUserDetails principal) {
        return chartService.chart(period, limit, principal == null ? null : principal.id());
    }

    @GetMapping("/trending/now")
    @Operation(summary = "Most-discussed looks right now")
    public List<ChartEntryDto> trending(@RequestParam(defaultValue = "12") @Min(1) @Max(30) int limit,
                                        @AuthenticationPrincipal AppUserDetails principal) {
        return chartService.trending(limit, principal == null ? null : principal.id());
    }
}
