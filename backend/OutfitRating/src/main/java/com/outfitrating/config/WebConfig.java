package com.outfitrating.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.context.annotation.Configuration;
import org.jspecify.annotations.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Serves uploaded images straight off the storage directory configured in {@code app.storage}. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties properties;

    public WebConfig(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        Path root = Paths.get(properties.storage().location()).toAbsolutePath().normalize();
        registry.addResourceHandler(properties.storage().publicPath() + "/**")
                .addResourceLocations(root.toUri().toString())
                .setCachePeriod(60 * 60 * 24);
    }
}
