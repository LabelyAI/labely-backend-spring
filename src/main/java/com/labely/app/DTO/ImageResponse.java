package com.labely.app.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Metadata for an uploaded image")
public class ImageResponse {
    @Schema(example = "1")
    private Long id;
    @Schema(example = "photo.jpg")
    private String fileName;
    @Schema(example = "https://cdn.example.com/photo.jpg")
    private String fileUrl;
    @Schema(example = "image/jpeg")
    private String contentType;
    @Schema(example = "204800", description = "File size in bytes")
    private Long fileSize;
    @Schema(example = "My vacation photo")
    private String description;
    @Schema(example = "PENDING")
    private String status;
    private LocalDateTime uploadedAt;
    private LocalDateTime annotatedAt;

    public ImageResponse() {}

    public ImageResponse(Long id, String fileName, String fileUrl, String contentType, Long fileSize,
                         String description, String status, LocalDateTime uploadedAt, LocalDateTime annotatedAt) {
        this.id = id;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.description = description;
        this.status = status;
        this.uploadedAt = uploadedAt;
        this.annotatedAt = annotatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public LocalDateTime getAnnotatedAt() { return annotatedAt; }
    public void setAnnotatedAt(LocalDateTime annotatedAt) { this.annotatedAt = annotatedAt; }
}
