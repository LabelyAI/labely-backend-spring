package com.labely.app.Service;

import com.labely.app.DTO.AnnotationResponse;
import com.labely.app.DTO.AnnotationRunRequest;
import com.labely.app.DTO.DetectionDTO;
import com.labely.app.DTO.Sam3Response;
import com.labely.app.Entity.Annotation;
import com.labely.app.Entity.AnnotationStatus;
import com.labely.app.Entity.Detection;
import com.labely.app.Entity.ImageMetadata;
import com.labely.app.Entity.ImageStatus;
import com.labely.app.Entity.User;
import com.labely.app.Repository.AnnotationRepository;
import com.labely.app.Repository.ImageMetadataRepository;
import com.labely.app.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnnotationService {

    @Autowired
    private Sam3Client sam3Client;

    @Autowired
    private R2StorageService r2StorageService;

    @Autowired
    private AnnotationRepository annotationRepository;

    @Autowired
    private ImageMetadataRepository imageMetadataRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${sam3.default-mode:sam3}")
    private String defaultMode;

    @Value("${sam3.default-conf-threshold:0.35}")
    private Double defaultConfThreshold;

    @Transactional
    public List<AnnotationResponse> run(AnnotationRunRequest request, String userEmail) {
        if (request.getImageIds() == null || request.getImageIds().isEmpty()) {
            throw new RuntimeException("imageIds is required");
        }
        if (request.getPrompt() == null || request.getPrompt().isBlank()) {
            throw new RuntimeException("prompt is required");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String mode = (request.getMode() == null || request.getMode().isBlank()) ? defaultMode : request.getMode();
        double confThreshold = request.getConfThreshold() != null ? request.getConfThreshold() : defaultConfThreshold;
        boolean largestComponent = request.getLargestComponent() == null || request.getLargestComponent();
        boolean returnImages = request.getReturnImages() == null || request.getReturnImages();

        List<ImageMetadata> images = imageMetadataRepository.findByUserAndIdIn(user, request.getImageIds());
        if (images.isEmpty()) {
            throw new RuntimeException("No matching images found for this user");
        }

        List<AnnotationResponse> results = new ArrayList<>();
        for (ImageMetadata image : images) {
            Annotation annotation = annotateSingleImage(image, user, request.getPrompt(), mode,
                    confThreshold, largestComponent, returnImages);
            results.add(toResponse(annotation));
        }
        return results;
    }

    private Annotation annotateSingleImage(ImageMetadata image, User user, String prompt, String mode,
                                           double confThreshold, boolean largestComponent, boolean returnImages) {
        image.setStatus(ImageStatus.PROCESSING);
        imageMetadataRepository.save(image);

        try {
            byte[] imageBytes = r2StorageService.downloadImage(image.getR2Key());
            Sam3Response sam3 = sam3Client.annotate(imageBytes, image.getFileName(), image.getContentType(),
                    prompt, mode, confThreshold, largestComponent, returnImages);

            Annotation annotation = new Annotation();
            annotation.setImage(image);
            annotation.setUser(user);
            annotation.setPrompt(prompt);
            annotation.setMode(mode);
            annotation.setConfThreshold(confThreshold);
            annotation.setStatus(AnnotationStatus.UNREVIEWED);
            annotation.setImageWidth(sam3.getImageSize() != null ? sam3.getImageSize().getW() : 0);
            annotation.setImageHeight(sam3.getImageSize() != null ? sam3.getImageSize().getH() : 0);
            annotation.setNumInstances(sam3.getNumInstances() != null ? sam3.getNumInstances() : 0);

            if (returnImages && sam3.getOverlayPngB64() != null && !sam3.getOverlayPngB64().isBlank()) {
                byte[] overlayBytes = Base64.getDecoder().decode(sam3.getOverlayPngB64());
                String overlayKey = r2StorageService.uploadBytes(overlayBytes, "overlays", ".png", "image/png");
                annotation.setOverlayR2Key(overlayKey);
                annotation.setOverlayUrl(r2StorageService.publicUrlFor(overlayKey));
            }
            if (returnImages && sam3.getMaskPngB64() != null && !sam3.getMaskPngB64().isBlank()) {
                byte[] maskBytes = Base64.getDecoder().decode(sam3.getMaskPngB64());
                String maskKey = r2StorageService.uploadBytes(maskBytes, "masks", ".png", "image/png");
                annotation.setMaskR2Key(maskKey);
                annotation.setMaskUrl(r2StorageService.publicUrlFor(maskKey));
            }

            List<Detection> detections = new ArrayList<>();
            if (sam3.getDetections() != null) {
                for (Sam3Response.Sam3Detection d : sam3.getDetections()) {
                    Detection det = new Detection(annotation, prompt,
                            d.getX1(), d.getY1(), d.getX2(), d.getY2(), d.getScore());
                    detections.add(det);
                }
            }
            annotation.setDetections(detections);

            Annotation saved = annotationRepository.save(annotation);

            image.setStatus(ImageStatus.ANNOTATED);
            image.setAnnotatedAt(LocalDateTime.now());
            imageMetadataRepository.save(image);

            return saved;
        } catch (Exception e) {
            image.setStatus(ImageStatus.FAILED);
            imageMetadataRepository.save(image);
            throw new RuntimeException("Annotation failed for image " + image.getId() + ": " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<AnnotationResponse> listForUser(String userEmail, AnnotationStatus status) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Annotation> list = status == null
                ? annotationRepository.findByUserOrderByCreatedAtDesc(user)
                : annotationRepository.findByUserAndStatusOrderByCreatedAtDesc(user, status);
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AnnotationResponse getById(Long id, String userEmail) {
        Annotation a = annotationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Annotation not found"));
        if (!a.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized");
        }
        return toResponse(a);
    }

    @Transactional(readOnly = true)
    public List<AnnotationResponse> getByImageId(Long imageId, String userEmail) {
        ImageMetadata image = imageMetadataRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found"));
        if (!image.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized");
        }
        return annotationRepository.findByImageOrderByCreatedAtDesc(image).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public AnnotationResponse review(Long id, String decision, String userEmail) {
        Annotation a = annotationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Annotation not found"));
        if (!a.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized");
        }
        AnnotationStatus newStatus;
        try {
            newStatus = AnnotationStatus.valueOf(decision.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid decision: " + decision);
        }
        a.setStatus(newStatus);
        a.setReviewedAt(LocalDateTime.now());
        return toResponse(annotationRepository.save(a));
    }

    @Transactional
    public void delete(Long id, String userEmail) {
        Annotation a = annotationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Annotation not found"));
        if (!a.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized");
        }
        if (a.getOverlayR2Key() != null) r2StorageService.deleteObject(a.getOverlayR2Key());
        if (a.getMaskR2Key() != null) r2StorageService.deleteObject(a.getMaskR2Key());
        annotationRepository.delete(a);
    }

    @Transactional
    public int transferApproved(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Annotation> approved = annotationRepository
                .findByUserAndStatusAndTransferredFalseOrderByCreatedAtDesc(user, AnnotationStatus.APPROVED);
        for (Annotation a : approved) {
            a.setTransferred(true);
        }
        annotationRepository.saveAll(approved);
        return approved.size();
    }

    public AnnotationResponse toResponse(Annotation a) {
        AnnotationResponse r = new AnnotationResponse();
        r.setId(a.getId());
        r.setImageId(a.getImage().getId());
        r.setImageFileName(a.getImage().getFileName());
        r.setImageUrl(a.getImage().getFileUrl());
        r.setPrompt(a.getPrompt());
        r.setMode(a.getMode());
        r.setConfThreshold(a.getConfThreshold());
        r.setStatus(a.getStatus().name());
        r.setImageWidth(a.getImageWidth());
        r.setImageHeight(a.getImageHeight());
        r.setNumInstances(a.getNumInstances());
        r.setOverlayUrl(a.getOverlayUrl());
        r.setMaskUrl(a.getMaskUrl());
        r.setTransferred(a.getTransferred());
        r.setCreatedAt(a.getCreatedAt());
        r.setReviewedAt(a.getReviewedAt());
        r.setDetections(a.getDetections() == null ? List.of() :
                a.getDetections().stream()
                        .map(d -> new DetectionDTO(d.getId(), d.getLabel(),
                                d.getX1(), d.getY1(), d.getX2(), d.getY2(), d.getScore()))
                        .collect(Collectors.toList()));
        return r;
    }
}
