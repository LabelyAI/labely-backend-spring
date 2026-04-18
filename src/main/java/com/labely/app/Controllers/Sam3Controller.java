package com.labely.app.Controllers;

import com.labely.app.DTO.Sam3Response;
import com.labely.app.Service.Sam3Client;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/sam3")
@Tag(name = "SAM3", description = "Direct SAM3 FastAPI proxy (no persistence)")
public class Sam3Controller {

    @Autowired
    private Sam3Client sam3Client;

    @Operation(summary = "Check if SAM3 service is reachable")
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        try {
            return ResponseEntity.ok(Map.of(
                    "reachable", sam3Client.isHealthy(),
                    "body", sam3Client.health()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("reachable", false, "error", e.getMessage()));
        }
    }

    @Operation(summary = "Proxy /annotate directly without persisting")
    @PostMapping(value = "/annotate", consumes = "multipart/form-data")
    public ResponseEntity<?> annotate(
            @RequestParam("file") MultipartFile file,
            @RequestParam("prompt") String prompt,
            @RequestParam(value = "mode", defaultValue = "sam3") String mode,
            @RequestParam(value = "conf_thresh", defaultValue = "0.35") Double confThresh,
            @RequestParam(value = "largest_component", defaultValue = "true") Boolean largestComponent,
            @RequestParam(value = "return_images", defaultValue = "true") Boolean returnImages) {
        try {
            Sam3Response r = sam3Client.annotate(file.getBytes(), file.getOriginalFilename(),
                    file.getContentType(), prompt, mode, confThresh, largestComponent, returnImages);
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
