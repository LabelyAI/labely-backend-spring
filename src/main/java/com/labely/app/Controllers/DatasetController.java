package com.labely.app.Controllers;

import com.labely.app.Config.JwtUtil;
import com.labely.app.DTO.DatasetStatsResponse;
import com.labely.app.DTO.ExportRequest;
import com.labely.app.Service.DatasetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@RestController
@RequestMapping("/api/dataset")
@Tag(name = "Dataset", description = "Dataset stats and exports")
public class DatasetController {

    @Autowired
    private DatasetService datasetService;

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

    @Operation(summary = "Get dataset counts for current user", security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Dataset stats",
                content = @Content(schema = @Schema(implementation = DatasetStatsResponse.class)))
        })
    @GetMapping("/stats")
    public ResponseEntity<?> stats(@Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        try {
            String email = authenticate(authHeader);
            return ResponseEntity.ok(datasetService.getStats(email));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @Operation(summary = "Export dataset in the requested format (zip)",
        security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/export")
    public ResponseEntity<?> export(
            @RequestBody ExportRequest request,
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        try {
            String email = authenticate(authHeader);
            boolean approvedOnly = request.getApprovedOnly() == null || request.getApprovedOnly();
            byte[] zip = datasetService.export(email, request.getFormat(), request.getAnnotationIds(), approvedOnly);
            String fmt = request.getFormat() == null ? "yolo" : request.getFormat().toLowerCase();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"labely-export-" + fmt + ".zip\"")
                    .body(zip);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
