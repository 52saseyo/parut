package com.parut.product.image.infrastructure.persistence;

import com.parut.product.image.domain.productImage.ProductImage;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductImageRepository extends CrudRepository<ProductImage, UUID> {

    Optional<ProductImage> findByProductIdAndDeletedAtIsNull(UUID productId);

    List<ProductImage> findAllByProductIdAndDeletedAtIsNull(UUID productId);

    boolean existsByProductIdAndDeletedAtIsNull(UUID productId);

}
