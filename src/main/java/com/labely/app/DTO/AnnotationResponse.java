package com.labely.app.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Annotation result for an image")
public class AnnotationResponse {
    private Long id;
    private Long imageId;
    private String imageFileName;
    private String imageUrl;
    private String prompt;
    private String mode;
    private Double confThreshold;
    @Schema(example = "UNREVIEWED")
    private String status;
    private Integer imageWidth;
    private Integer imageHeight;
    private Integer numInstances;
    private String overlayUrl;
    private String maskUrl;
    private Boolean transferred;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private List<DetectionDTO> detections;

    public AnnotationResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getImageId() { return imageId; }
    public void setImageId(Long imageId) { this.imageId = imageId; }
    public String getImageFileName() { return imageFileName; }
    public void setImageFileName(String imageFileName) { this.imageFileName = imageFileName; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public Double getConfThreshold() { return confThreshold; }
    public void setConfThreshold(Double confThreshold) { this.confThreshold = confThreshold; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getImageWidth() { return imageWidth; }
    public void setImageWidth(Integer imageWidth) { this.imageWidth = imageWidth; }
    public Integer getImageHeight() { return imageHeight; }
    public void setImageHeight(Integer imageHeight) { this.imageHeight = imageHeight; }
    public Integer getNumInstances() { return numInstances; }
    public void setNumInstances(Integer numInstances) { this.numInstances = numInstances; }
    public String getOverlayUrl() { return overlayUrl; }
    public void setOverlayUrl(String overlayUrl) { this.overlayUrl = overlayUrl; }
    public String getMaskUrl() { return maskUrl; }
    public void setMaskUrl(String maskUrl) { this.maskUrl = maskUrl; }
    public Boolean getTransferred() { return transferred; }
    public void setTransferred(Boolean transferred) { this.transferred = transferred; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public List<DetectionDTO> getDetections() { return detections; }
    public void setDetections(List<DetectionDTO> detections) { this.detections = detections; }
}
