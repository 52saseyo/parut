package com.parut.product.image.application.service;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.image.application.dto.LinkedImage;
import com.parut.product.image.domain.image.Image;
import com.parut.product.image.domain.productImage.ProductImage;
import com.parut.product.image.infrastructure.persistence.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductImageService
{
    private final ImageService imageService;
    private final ProductImageRepository productImageRepository;

    @Transactional
    public void registerImage(UUID uploaderId, UUID productId, UUID imageId){
        boolean alreadyExists =
                productImageRepository.existsByProductIdAndDeletedAtIsNull(productId);
        if(alreadyExists){
            throw new BusinessException(ErrorCode.PRODUCT_IMAGE_ALREADY_EXISTS);
        }

        Image image = imageService.getOwnedImage(uploaderId, imageId);

        ProductImage productImage = ProductImage.create(productId, image.getId(), image.getImageUrl());

        productImageRepository.save(productImage);
    }


    @Transactional(readOnly = true)
    public Optional<LinkedImage> getImageInfo(UUID productId){
        return productImageRepository
                .findByProductIdAndDeletedAtIsNull(productId)
                .map(link -> new LinkedImage(
                        link.getImageId(),
                        link.getImageUrl()
                ));
    }


    @Transactional(readOnly = true)
    public boolean hasImage(UUID productId){
        return productImageRepository.existsByProductIdAndDeletedAtIsNull(productId);
    }


    @Transactional(readOnly = true)
    public List<String> getImageUrls(UUID productId){
        return productImageRepository.
                findAllByProductIdAndDeletedAtIsNull(productId)
                .stream()
                .map(ProductImage::getImageUrl)
                .toList();
    }



}
