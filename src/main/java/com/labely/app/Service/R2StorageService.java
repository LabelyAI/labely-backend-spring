package com.labely.app.Service;

import com.labely.app.Entity.ImageMetadata;
import com.labely.app.Entity.User;
import com.labely.app.Repository.ImageMetadataRepository;
import com.labely.app.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class R2StorageService {

    @Autowired
    private S3Client s3Client;

    @Autowired
    private ImageMetadataRepository imageMetadataRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${cloudflare.r2.bucket-name}")
    private String bucketName;

    @Value("${cloudflare.r2.public-url}")
    private String publicUrl;

    public ImageMetadata uploadImage(MultipartFile file, String userEmail, String description) throws IOException {
        // Get user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate unique key for R2
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String r2Key = "images/" + UUID.randomUUID().toString() + fileExtension;

        // Upload to R2
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(r2Key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

        // Create file URL
        String fileUrl = publicUrl + "/" + r2Key;

        // Save metadata to database
        ImageMetadata metadata = new ImageMetadata(
                file.getOriginalFilename(),
                r2Key,
                fileUrl,
                file.getContentType(),
                file.getSize(),
                user
        );
        metadata.setDescription(description);

        return imageMetadataRepository.save(metadata);
    }

    public List<ImageMetadata> getUserImages(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return imageMetadataRepository.findByUser(user);
    }

    public ImageMetadata getImageById(Long id) {
        return imageMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found"));
    }

    public void deleteImage(Long id, String userEmail) {
        ImageMetadata metadata = imageMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        // Verify ownership
        if (!metadata.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized to delete this image");
        }

        // Delete from R2
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(metadata.getR2Key())
                .build();

        s3Client.deleteObject(deleteObjectRequest);

        // Delete metadata from database
        imageMetadataRepository.delete(metadata);
    }

    public byte[] downloadImage(String r2Key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(r2Key)
                .build();

        return s3Client.getObjectAsBytes(getObjectRequest).asByteArray();
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
