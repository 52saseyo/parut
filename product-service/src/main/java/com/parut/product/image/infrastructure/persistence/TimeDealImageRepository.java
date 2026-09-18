package com.parut.product.image.infrastructure.persistence;

import com.parut.product.image.domain.timeDealImage.TimeDealImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TimeDealImageRepository extends JpaRepository<TimeDealImage, UUID> {

    Optional<TimeDealImage> findByTimeDealIdAndDeletedAtIsNull(UUID timeDealId);

    boolean existsByTimeDealIdAndDeletedAtIsNull(UUID timeDealId);
 }
