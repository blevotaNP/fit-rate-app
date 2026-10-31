package com.outfitrating.service;

import com.outfitrating.dto.UploadResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Where uploaded imagery lives. The only implementation writes to the local disk; the interface
 * exists so swapping in S3 or any object store is a one-class change rather than a refactor.
 */
public interface StorageService {

    UploadResponse store(MultipartFile file);

    void delete(String url);
}
