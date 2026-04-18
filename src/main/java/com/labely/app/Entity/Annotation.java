package com.labely.app.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "annotations")
public class Annotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false)
    private ImageMetadata image;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String prompt;

    @Column(nullable = false)
    private String mode;

    @Column(nullable = false)
    private Double confThreshold;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnnotationStatus status = AnnotationStatus.UNREVIEWED;

    @Column(nullable = false)
    private Integer imageWidth;

    @Column(nullable = false)
    private Integer imageHeight;

    @Column(nullable = false)
    private Integer numInstances;

    private String overlayUrl;

    private String overlayR2Key;

    private String maskUrl;

    private String maskR2Key;

    @Column(nullable = false)
    private Boolean transferred = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;

    @OneToMany(mappedBy = "annotation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Detection> detections = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = AnnotationStatus.UNREVIEWED;
        if (transferred == null) transferred = false;
    }

    public Annotation() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ImageMetadata getImage() { return image; }
    public void setImage(ImageMetadata image) { this.image = image; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public Double getConfThreshold() { return confThreshold; }
    public void setConfThreshold(Double confThreshold) { this.confThreshold = confThreshold; }
    public AnnotationStatus getStatus() { return status; }
    public void setStatus(AnnotationStatus status) { this.status = status; }
    public Integer getImageWidth() { return imageWidth; }
    public void setImageWidth(Integer imageWidth) { this.imageWidth = imageWidth; }
    public Integer getImageHeight() { return imageHeight; }
    public void setImageHeight(Integer imageHeight) { this.imageHeight = imageHeight; }
    public Integer getNumInstances() { return numInstances; }
    public void setNumInstances(Integer numInstances) { this.numInstances = numInstances; }
    public String getOverlayUrl() { return overlayUrl; }
    public void setOverlayUrl(String overlayUrl) { this.overlayUrl = overlayUrl; }
    public String getOverlayR2Key() { return overlayR2Key; }
    public void setOverlayR2Key(String overlayR2Key) { this.overlayR2Key = overlayR2Key; }
    public String getMaskUrl() { return maskUrl; }
    public void setMaskUrl(String maskUrl) { this.maskUrl = maskUrl; }
    public String getMaskR2Key() { return maskR2Key; }
    public void setMaskR2Key(String maskR2Key) { this.maskR2Key = maskR2Key; }
    public Boolean getTransferred() { return transferred; }
    public void setTransferred(Boolean transferred) { this.transferred = transferred; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public List<Detection> getDetections() { return detections; }
    public void setDetections(List<Detection> detections) { this.detections = detections; }
}
