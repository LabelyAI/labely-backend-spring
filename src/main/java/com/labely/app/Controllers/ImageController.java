package com.labely.app.Controllers;

import com.labely.app.Config.JwtUtil;
import com.labely.app.DTO.ImageResponse;
import com.labely.app.Entity.ImageMetadata;
import com.labely.app.Service.R2StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    @Autowired
    private R2StorageService r2StorageService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String userEmail = jwtUtil.extractUsername(token);

            if (!jwtUtil.validateToken(token, userEmail)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }

            ImageMetadata metadata = r2StorageService.uploadImage(file, userEmail, description);
            
            ImageResponse response = new ImageResponse(
                metadata.getId(),
                metadata.getFileName(),
                metadata.getFileUrl(),
                metadata.getContentType(),
                metadata.getFileSize(),
                metadata.getDescription(),
                metadata.getUploadedAt()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/my-images")
    public ResponseEntity<?> getUserImages(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String userEmail = jwtUtil.extractUsername(token);

            if (!jwtUtil.validateToken(token, userEmail)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }

            List<ImageMetadata> images = r2StorageService.getUserImages(userEmail);
            
            List<ImageResponse> response = images.stream()
                    .map(img -> new ImageResponse(
                        img.getId(),
                        img.getFileName(),
                        img.getFileUrl(),
                        img.getContentType(),
                        img.getFileSize(),
                        img.getDescription(),
                        img.getUploadedAt()
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getImage(@PathVariable Long id) {
        try {
            ImageMetadata metadata = r2StorageService.getImageById(id);
            
            ImageResponse response = new ImageResponse(
                metadata.getId(),
                metadata.getFileName(),
                metadata.getFileUrl(),
                metadata.getContentType(),
                metadata.getFileSize(),
                metadata.getDescription(),
                metadata.getUploadedAt()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteImage(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String userEmail = jwtUtil.extractUsername(token);

            if (!jwtUtil.validateToken(token, userEmail)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }

            r2StorageService.deleteImage(id, userEmail);
            return ResponseEntity.ok("Image deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

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
