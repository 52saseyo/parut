package com.parut.product.image.application.service;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.image.domain.image.Image;
import com.parut.product.image.presentation.dto.request.RegisterProductImageRequest;
import com.parut.product.image.presentation.dto.response.ImageResponse;
import com.parut.product.product.domain.product.Product;
import com.parut.product.product.infrastructure.product.persistence.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductImageService
{
    private final ProductRepository productRepository;
    private final ImageService imageService;

    /**
     * 판매자의 상품에 최초 이미지를 등록한다.
     */
    @Transactional
    public ImageResponse addImage(
            UUID sellerId,
            UUID productId,
            RegisterProductImageRequest request
    ) {
        Product product = productRepository
                .findByIdAndSellerIdAndDeletedAtIsNull(productId, sellerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        Image image = imageService.create(
                request.imageKey(),
                request.originalName(),
                request.contentType(),
                request.fileSize()
        );

        product.addImage(image.getId());

        return ImageResponse.from(image);
    }
}
