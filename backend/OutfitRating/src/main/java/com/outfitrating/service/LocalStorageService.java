package com.outfitrating.service;

import com.outfitrating.config.AppProperties;
import com.outfitrating.dto.UploadResponse;
import com.outfitrating.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/** Disk-backed {@link StorageService}: files land under {@code app.storage.location}. */
@Service
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);
    private static final DateTimeFormatter MONTH_FOLDER = DateTimeFormatter.ofPattern("yyyy/MM");

    private final AppProperties.Storage config;
    private final Path root;

    public LocalStorageService(AppProperties properties) {
        this.config = properties.storage();
        this.root = Paths.get(config.location()).toAbsolutePath().normalize();
    }

    @PostConstruct
    void createRoot() throws IOException {
        Files.createDirectories(root);
        log.info("Image uploads are stored in {}", root);
    }

    @Override
    public UploadResponse store(MultipartFile file) {
        validate(file);
        String extension = extensionFor(file);
        String relative = LocalDate.now().format(MONTH_FOLDER) + "/" + UUID.randomUUID() + extension;
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw new BadRequestException("Resolved upload path escapes the storage directory");
        }
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not store the uploaded file", ex);
        }
        return new UploadResponse(
                config.publicPath() + "/" + relative,
                relative,
                file.getSize(),
                file.getContentType());
    }

    @Override
    public void delete(String url) {
        if (url == null || !url.startsWith(config.publicPath() + "/")) {
            return;
        }
        Path target = root.resolve(url.substring(config.publicPath().length() + 1)).normalize();
        if (!target.startsWith(root)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            log.warn("Could not delete {}: {}", target, ex.getMessage());
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file was uploaded");
        }
        if (file.getSize() > config.maxFileSizeBytes()) {
            throw new BadRequestException("The image must be smaller than "
                    + (config.maxFileSizeBytes() / (1024 * 1024)) + " MB");
        }
        List<String> allowed = config.allowedContentTypes();
        String contentType = file.getContentType();
        if (contentType == null || allowed.stream().noneMatch(contentType::equalsIgnoreCase)) {
            throw new BadRequestException("Unsupported image type; allowed: " + String.join(", ", allowed));
        }
    }

    private String extensionFor(MultipartFile file) {
        String original = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(original);
        if (extension == null || extension.isBlank() || extension.length() > 5) {
            return switch (file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT)) {
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                case "image/avif" -> ".avif";
                default -> ".jpg";
            };
        }
        return "." + extension.toLowerCase(Locale.ROOT);
    }
}
