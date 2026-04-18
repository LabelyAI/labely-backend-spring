package com.labely.app.Repository;

import com.labely.app.Entity.ImageMetadata;
import com.labely.app.Entity.ImageStatus;
import com.labely.app.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ImageMetadataRepository extends JpaRepository<ImageMetadata, Long> {
    List<ImageMetadata> findByUserOrderByUploadedAtDesc(User user);
    List<ImageMetadata> findByUser(User user);
    List<ImageMetadata> findByUserAndStatusOrderByUploadedAtDesc(User user, ImageStatus status);
    List<ImageMetadata> findByUserAndIdIn(User user, List<Long> ids);
    List<ImageMetadata> findByUserId(Long userId);
    long countByUser(User user);
    long countByUserAndStatus(User user, ImageStatus status);
    Optional<ImageMetadata> findByR2Key(String r2Key);
}
