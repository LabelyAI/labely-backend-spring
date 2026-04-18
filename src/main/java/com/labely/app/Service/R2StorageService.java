package com.labely.app.Service;

import com.labely.app.Entity.ImageMetadata;
import com.labely.app.Entity.ImageStatus;
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
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String fileExtension = getFileExtension(file.getOriginalFilename());
        String r2Key = "images/" + UUID.randomUUID().toString() + fileExtension;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(r2Key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

        String fileUrl = publicUrl + "/" + r2Key;

        ImageMetadata metadata = new ImageMetadata(
                file.getOriginalFilename(),
                r2Key,
                fileUrl,
                file.getContentType(),
                file.getSize(),
                user
        );
        metadata.setDescription(description);
        metadata.setStatus(ImageStatus.PENDING);

        return imageMetadataRepository.save(metadata);
    }

    public String uploadBytes(byte[] data, String keyPrefix, String extension, String contentType) {
        String key = keyPrefix + "/" + UUID.randomUUID().toString() + (extension.startsWith(".") ? extension : "." + extension);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(data));
        return key;
    }

    public String publicUrlFor(String r2Key) {
        return publicUrl + "/" + r2Key;
    }

    public List<ImageMetadata> getUserImages(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return imageMetadataRepository.findByUserOrderByUploadedAtDesc(user);
    }

    public List<ImageMetadata> getUserImagesByStatus(String userEmail, ImageStatus status) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return imageMetadataRepository.findByUserAndStatusOrderByUploadedAtDesc(user, status);
    }

    public ImageMetadata getImageById(Long id) {
        return imageMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found"));
    }

    public void deleteImage(Long id, String userEmail) {
        ImageMetadata metadata = imageMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        if (!metadata.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized to delete this image");
        }

        deleteObject(metadata.getR2Key());

        imageMetadataRepository.delete(metadata);
    }

    public void deleteObject(String r2Key) {
        if (r2Key == null || r2Key.isBlank()) return;
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(r2Key)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
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
