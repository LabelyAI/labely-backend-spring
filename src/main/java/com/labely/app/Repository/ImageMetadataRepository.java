package com.labely.app.Repository;

import com.labely.app.Entity.ImageMetadata;
import com.labely.app.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ImageMetadataRepository extends JpaRepository<ImageMetadata, Long> {
    List<ImageMetadata> findByUser(User user);
    List<ImageMetadata> findByUserId(Long userId);
    Optional<ImageMetadata> findByR2Key(String r2Key);
}
