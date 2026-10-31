package com.outfitrating.web;

import com.outfitrating.dto.UploadResponse;
import com.outfitrating.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
@Tag(name = "Uploads")
public class UploadController {

    private final StorageService storage;

    public UploadController(StorageService storage) {
        this.storage = storage;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an image and get back the URL to attach to a look")
    public UploadResponse upload(@RequestPart("file") MultipartFile file) {
        return storage.store(file);
    }
}
