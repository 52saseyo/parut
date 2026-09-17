package com.parut.product.image.application.service;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.image.application.authorization.ImageAuthorizationChecker;
import com.parut.product.image.application.dto.ImageUploadUrlResult;
import com.parut.product.image.application.dto.UploadedImage;
import com.parut.product.image.domain.image.Image;
import com.parut.product.image.infrastructure.persistence.ImageRepository;
import com.parut.product.image.presentation.dto.request.ImageUploadCompleteRequest;
import com.parut.product.image.presentation.dto.request.ImageUploadUrlRequest;
import com.parut.product.image.presentation.dto.response.ImageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final ImageRepository imageRepository;
    private final S3ImageService  s3ImageService;
    private final ImageAuthorizationChecker authorizationChecker;


    public ImageUploadUrlResult createUploadUrl(
            UUID requesterId,
            String requesterRole,
            ImageUploadUrlRequest request
    ) {
        authorizationChecker.requireUploaderRole(requesterRole);
        return s3ImageService.createUploadUrl(requesterId, request.contentType(), request.fileSize());
    }


    @Transactional
    public ImageResponse completeUpload(
            UUID requesterId,
            String requesterRole,
            ImageUploadCompleteRequest request
    ) {
        authorizationChecker.requireUploaderRole(requesterRole);

        if(imageRepository.existsByImageKey(request.imageKey())){
            throw new BusinessException(ErrorCode.IMAGE_KEY_ALREADY_EXISTS);
        }

        UploadedImage uploaded = s3ImageService.checkS3UploadedImage(requesterId, request.imageKey());

        Image image = Image.create(
                requesterId,
                uploaded.imageKey(),
                uploaded.imageUrl(),
                request.originalName(),
                uploaded.contentType(),
                uploaded.fileSize()
        );

        Image savedImage = imageRepository.save(image);

        return ImageResponse.from(savedImage);
    }

    public Image getOwnedImage(UUID uploaderId, UUID imageId) {
        return imageRepository
                .findByIdAndUploaderIdAndDeletedAtIsNull(imageId, uploaderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));

    }

}
