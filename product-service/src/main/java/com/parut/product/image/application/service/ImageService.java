package com.parut.product.image.application.service;


import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.image.domain.image.Image;
import com.parut.product.image.infrastructure.persistence.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final ImageRepository imageRepository;


    @Transactional
    public Image create(
            String imageKey,
            String originalName,
            String contentType,
            long fileSize
    ) {
        if (imageRepository.existsByImageKey(imageKey)) {
            throw new BusinessException(
                    ErrorCode.IMAGE_ALREADY_EXISTS
            );
        }

        Image image = Image.create(
                imageKey,
                originalName,
                contentType,
                fileSize
        );

        return imageRepository.save(image);
    }

}
