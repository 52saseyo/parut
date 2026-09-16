package com.parut.product.image.application.service;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.image.application.dto.ImageUploadUrlResult;
import com.parut.product.image.application.dto.UploadedImage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class S3ImageService {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> SUPPORTED_CONTENT_TYPES =
            Set.of(
                    "image/jpeg",
                    "image/png",
                    "image/webp"
            );

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;


    @Value("${cloud.aws.s3.bucket}")
    private String bucket;


    @Value("${cloud.aws.s3.base-url}")
    private String baseUrl;


    @Value("${cloud.aws.s3.presigned-expiration-seconds}")
    private long expirationSeconds;

    public ImageUploadUrlResult createUploadUrl(
            UUID uploaderId,
            String contentType,
            long fileSize
    ){
        validateUploaderId(uploaderId);
        validateMetadata(contentType, fileSize);

        String imageKey = createImageKey(uploaderId, contentType);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(imageKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expirationSeconds))
                .putObjectRequest(putObjectRequest)
                .build();

        String uploadUrl = s3Presigner.presignPutObject(presignRequest)
                .url()
                .toString();

        return new  ImageUploadUrlResult(imageKey, uploadUrl);
    }

    public UploadedImage checkS3UploadedImage(UUID uploaderId,  String imageKey){
        validateUploaderId(uploaderId);
        validateImageKeyOwner(uploaderId, imageKey);

        try {
            HeadObjectResponse response = s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucket)
                            .key(imageKey)
                            .build()
            );

            validateMetadata(response.contentType(), response.contentLength());
            String imageUrl = createImageUrl(imageKey);
            return new UploadedImage(
                    imageKey,
                    imageUrl,
                    response.contentType(),
                    response.contentLength()
            );

        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new BusinessException(
                        ErrorCode.IMAGE_UPLOAD_NOT_FOUND
                );
            }
            throw exception;
        }

    }

    private String createImageKey(UUID uploaderId, String contentType) {
        return "images/%s/%s.%s".formatted(
                uploaderId,
                UUID.randomUUID(),
                extensionOf(contentType)
        );
    }

    private String createImageUrl(String imageKey){
        return "%s/%s".formatted(baseUrl, imageKey);
    }


    private void validateImageKeyOwner(UUID uploaderId, String imageKey) {
        String expectedPrefix = "images/%s/".formatted(uploaderId);

        if(imageKey == null || !imageKey.startsWith(expectedPrefix)){
            throw new BusinessException(ErrorCode.IMAGE_INVALID_KEY);
        }
    }

    private void validateUploaderId(UUID uploaderId) {
        if (uploaderId == null) {
            throw new BusinessException(ErrorCode.IMAGE_INVALID_UPLOADER_ID);
        }
    }

    private void validateMetadata(String contentType, long fileSize) {
        if (!SUPPORTED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.IMAGE_INVALID_CONTENT_TYPE);
        }

        if (fileSize <= 0 || fileSize > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.IMAGE_INVALID_FILE_SIZE);
        }
    }

    private String extensionOf(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw new BusinessException(
                    ErrorCode.IMAGE_INVALID_CONTENT_TYPE
            );
        };
    }


}
