package com.labely.app.Controllers;

import com.labely.app.Config.JwtUtil;
import com.labely.app.DTO.ImageResponse;
import com.labely.app.Entity.ImageMetadata;
import com.labely.app.Entity.ImageStatus;
import com.labely.app.Service.R2StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/images")
@Tag(name = "Images", description = "Upload, retrieve, and manage images stored in Cloudflare R2")
public class ImageController {

    @Autowired
    private R2StorageService r2StorageService;

    @Autowired
    private JwtUtil jwtUtil;

    private String authenticate(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or malformed token");
        }
        String token = authHeader.substring(7);
        String email = jwtUtil.extractUsername(token);
        if (!jwtUtil.validateToken(token, email)) {
            throw new RuntimeException("Invalid token");
        }
        return email;
    }

    private ImageResponse toResponse(ImageMetadata m) {
        return new ImageResponse(
                m.getId(),
                m.getFileName(),
                m.getFileUrl(),
                m.getContentType(),
                m.getFileSize(),
                m.getDescription(),
                m.getStatus() == null ? null : m.getStatus().name(),
                m.getUploadedAt(),
                m.getAnnotatedAt()
        );
    }

    @Operation(summary = "Upload an image", security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Image uploaded",
                content = @Content(schema = @Schema(implementation = ImageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or missing token"),
            @ApiResponse(responseCode = "500", description = "Upload failed")
        })
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        try {
            String email = authenticate(authHeader);
            ImageMetadata metadata = r2StorageService.uploadImage(file, email, description);
            return ResponseEntity.ok(toResponse(metadata));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @Operation(summary = "Upload multiple images in a single request",
        security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/upload-batch")
    public ResponseEntity<?> uploadBatch(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "description", required = false) String description,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        try {
            String email = authenticate(authHeader);
            List<ImageResponse> out = new ArrayList<>();
            for (MultipartFile file : files) {
                ImageMetadata metadata = r2StorageService.uploadImage(file, email, description);
                out.add(toResponse(metadata));
            }
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @Operation(summary = "Get current user's images, optionally filtered by status",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "List of images",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = ImageResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Invalid or missing token")
        })
    @GetMapping("/my-images")
    public ResponseEntity<?> getUserImages(
            @RequestParam(value = "status", required = false) String status,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        try {
            String email = authenticate(authHeader);
            List<ImageMetadata> images;
            if (status != null && !status.isBlank()) {
                ImageStatus parsed;
                try { parsed = ImageStatus.valueOf(status.toUpperCase()); }
                catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body("Invalid status: " + status);
                }
                images = r2StorageService.getUserImagesByStatus(email, parsed);
            } else {
                images = r2StorageService.getUserImages(email);
            }
            return ResponseEntity.ok(images.stream().map(this::toResponse).collect(Collectors.toList()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @Operation(summary = "Get image metadata by ID")
    @GetMapping("/{id}")
    public ResponseEntity<?> getImage(@PathVariable Long id) {
        try {
            ImageMetadata metadata = r2StorageService.getImageById(id);
            return ResponseEntity.ok(toResponse(metadata));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @Operation(summary = "Delete an image", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteImage(
            @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        try {
            String email = authenticate(authHeader);
            r2StorageService.deleteImage(id, email);
            return ResponseEntity.ok(Map.of("status", "deleted", "id", id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @Operation(summary = "Delete multiple images", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/batch")
    public ResponseEntity<?> deleteImages(
            @RequestBody Map<String, List<Long>> body,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        try {
            String email = authenticate(authHeader);
            List<Long> ids = body.get("ids");
            if (ids == null || ids.isEmpty()) {
                return ResponseEntity.badRequest().body("ids required");
            }
            int deleted = 0;
            for (Long id : ids) {
                try { r2StorageService.deleteImage(id, email); deleted++; }
                catch (Exception ignored) { }
            }
            return ResponseEntity.ok(Map.of("deleted", deleted));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @Operation(summary = "Download image file by ID")
    @GetMapping("/download/{id}")
    public ResponseEntity<?> downloadImage(@PathVariable Long id) {
        try {
            ImageMetadata metadata = r2StorageService.getImageById(id);
            byte[] imageData = r2StorageService.downloadImage(metadata.getR2Key());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(metadata.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getFileName() + "\"")
                    .body(imageData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
