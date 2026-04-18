package com.labely.app.Repository;

import com.labely.app.Entity.Annotation;
import com.labely.app.Entity.AnnotationStatus;
import com.labely.app.Entity.ImageMetadata;
import com.labely.app.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnnotationRepository extends JpaRepository<Annotation, Long> {
    List<Annotation> findByUserOrderByCreatedAtDesc(User user);

    List<Annotation> findByUserAndStatusOrderByCreatedAtDesc(User user, AnnotationStatus status);

    List<Annotation> findByImageOrderByCreatedAtDesc(ImageMetadata image);

    Optional<Annotation> findFirstByImageOrderByCreatedAtDesc(ImageMetadata image);

    long countByUserAndStatus(User user, AnnotationStatus status);

    long countByUser(User user);

    List<Annotation> findByUserAndStatusAndTransferredFalseOrderByCreatedAtDesc(User user, AnnotationStatus status);

    List<Annotation> findByUserAndIdIn(User user, List<Long> ids);
}
