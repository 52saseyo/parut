package com.parut.product.image.infrastructure.persistence;

import com.parut.product.image.domain.image.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ImageRepository extends JpaRepository<Image, UUID> {
    boolean existsByImageKey(String imageKey);
}
