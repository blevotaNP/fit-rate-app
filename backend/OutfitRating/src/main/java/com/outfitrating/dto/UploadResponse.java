package com.outfitrating.dto;

public record UploadResponse(String url, String filename, long sizeBytes, String contentType) {
}
