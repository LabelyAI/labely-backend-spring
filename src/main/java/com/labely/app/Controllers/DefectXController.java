package com.labely.app.Controllers;

import com.labely.app.Config.JwtUtil;
import com.labely.app.DTO.DefectXReferenceRequest;
import com.labely.app.Entity.ImageMetadata;
import com.labely.app.Entity.User;
import com.labely.app.Repository.ImageMetadataRepository;
import com.labely.app.Repository.UserRepository;
import com.labely.app.Service.DefectXClient;
import com.labely.app.Service.R2StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/defectx")
@Tag(name = "DefectX", description = "Unsupervised defect detection (PatchCore + SAM3 region)")
public class DefectXController {

    @Autowired private DefectXClient defectXClient;
    @Autowired private R2StorageService r2StorageService;
    @Autowired private ImageMetadataRepository imageMetadataRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtil jwtUtil;

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

    @Operation(summary = "Check if DefectX service is reachable")
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        try {
            return ResponseEntity.ok(Map.of(
                    "reachable", defectXClient.isHealthy(),
                    "body", defectXClient.health()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("reachable", false, "error", e.getMessage()));
        }
    }

    @Operation(summary = "Register (or replace) the defect-free baseline for a user")
    @PostMapping("/reference")
    public ResponseEntity<?> setReference(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody DefectXReferenceRequest req) {
        try {
            String userEmail = authenticate(authHeader);

            if (req.getImageIds() == null || req.getImageIds().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "imageIds is required"));
            }
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<ImageMetadata> images = imageMetadataRepository.findByUserAndIdIn(user, req.getImageIds());
            if (images.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No matching images for user"));
            }

            List<byte[]> bytesList = new ArrayList<>();
            List<String> fileNames = new ArrayList<>();
            List<String> contentTypes = new ArrayList<>();
            for (ImageMetadata img : images) {
                bytesList.add(r2StorageService.downloadImage(img.getR2Key()));
                fileNames.add(img.getFileName());
                contentTypes.add(img.getContentType());
            }

            DefectXClient.ReferenceResponse resp =
                    defectXClient.setReference(bytesList, fileNames, contentTypes,
                            req.getProjectId(), req.getPrompt());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Delete a reference project from DefectX memory")
    @DeleteMapping("/reference/{projectId}")
    public ResponseEntity<?> deleteReference(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String projectId) {
        try {
            authenticate(authHeader);
            defectXClient.deleteReference(projectId);
            return ResponseEntity.ok(Map.of("deleted", true, "projectId", projectId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
