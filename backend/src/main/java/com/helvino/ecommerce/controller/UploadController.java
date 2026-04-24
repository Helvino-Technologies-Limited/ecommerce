package com.helvino.ecommerce.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/admin/upload")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Slf4j
public class UploadController {

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    @PostMapping("/image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
        }

        // If Cloudinary isn't configured, return a placeholder so the app still works
        if (cloudName == null || cloudName.isBlank()) {
            log.warn("Cloudinary not configured — returning placeholder image URL");
            return ResponseEntity.ok(Map.of(
                "url", "https://placehold.co/600x400?text=" + file.getOriginalFilename()
            ));
        }

        try {
            Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
            ));

            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", "helvino-shop/products",
                "resource_type", "image",
                "quality", "auto",
                "fetch_format", "auto"
            ));

            String url = (String) result.get("secure_url");
            log.info("Image uploaded to Cloudinary: {}", url);
            return ResponseEntity.ok(Map.of("url", url));

        } catch (Exception e) {
            log.error("Cloudinary upload failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }
}
