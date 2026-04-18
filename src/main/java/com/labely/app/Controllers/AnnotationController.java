package com.labely.app.Controllers;

import com.labely.app.Config.JwtUtil;
import com.labely.app.DTO.AnnotationResponse;
import com.labely.app.DTO.AnnotationRunRequest;
import com.labely.app.DTO.ManualAnnotationRequest;
import com.labely.app.DTO.ReviewDecisionRequest;
import com.labely.app.Entity.AnnotationStatus;
import com.labely.app.Service.AnnotationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/annotations")
@Tag(name = "Annotations", description = "Run SAM3 annotation, list, review, delete")
public class AnnotationController {

    @Autowired
    private AnnotationService annotationService;

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

    @Operation(summary = "Run SAM3 on a list of images", security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Annotations created",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = AnnotationResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Invalid token"),
            @ApiResponse(responseCode = "500", description = "Server error")
        })
    @PostMapping("/run")
    public ResponseEntity<?> run(
            @RequestBody AnnotationRunRequest request,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        try {
            String email = authenticate(authHeader);
            List<AnnotationResponse> results = annotationService.run(request, email);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @Operation(summary = "List annotations for current user, optionally filtered by status",
        security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(value = "status", required = false) String status,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        try {
            String email = authenticate(authHeader);
            AnnotationStatus parsed = null;
            if (status != null && !status.isBlank()) {
                try { parsed = AnnotationStatus.valueOf(status.toUpperCase()); }
                catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body("Invalid status: " + status);
                }
            }
            return ResponseEntity.ok(annotationService.listForUser(email, parsed));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @Operation(summary = "Get a single annotation by id", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
            @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        try {
            String email = authenticate(authHeader);
            return ResponseEntity.ok(annotationService.getById(id, email));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @Operation(summary = "Get all annotations for an image", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/image/{imageId}")
    public ResponseEntity<?> getByImage(
            @PathVariable Long imageId,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        try {
            String email = authenticate(authHeader);
            return ResponseEntity.ok(annotationService.getByImageId(imageId, email));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @Operation(summary = "Approve or reject an annotation", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/{id}/review")
    public ResponseEntity<?> review(
            @PathVariable Long id,
            @RequestBody ReviewDecisionRequest body,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        try {
            String email = authenticate(authHeader);
            if (body == null || body.getDecision() == null) {
                return ResponseEntity.badRequest().body("decision is required");
            }
            return ResponseEntity.ok(annotationService.review(id, body.getDecision(), email));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "Save manually drawn bounding boxes for a rejected annotation", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/{id}/manual")
    public ResponseEntity<?> saveManual(
            @PathVariable Long id,
            @RequestBody ManualAnnotationRequest body,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        try {
            String email = authenticate(authHeader);
            return ResponseEntity.ok(annotationService.saveManual(id, body, email));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "Delete an annotation", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        try {
            String email = authenticate(authHeader);
            annotationService.delete(id, email);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "Transfer all approved (un-transferred) annotations to final dataset",
        security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/transfer-approved")
    public ResponseEntity<?> transferApproved(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        try {
            String email = authenticate(authHeader);
            int n = annotationService.transferApproved(email);
            return ResponseEntity.ok(Map.of("transferred", n));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
