package com.outfitrating.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Strongly typed view over the {@code app.*} configuration namespace. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Storage storage, Cors cors, DemoData demoData) {

    public record Jwt(
            String secret,
            long accessTokenMinutes,
            long refreshTokenDays,
            String issuer,
            /** Set in production, where the refresh cookie must never cross a plain-HTTP hop. */
            boolean secureCookie) {
    }

    public record Storage(
            String location,
            String publicPath,
            long maxFileSizeBytes,
            List<String> allowedContentTypes) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record DemoData(boolean enabled) {
    }
}
