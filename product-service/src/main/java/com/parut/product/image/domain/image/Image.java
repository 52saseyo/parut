package com.parut.product.image.domain.image;

import com.parut.product.global.common.entity.DeletableEntity;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Getter
@Entity
@Table(name = "p_images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Image extends DeletableEntity {

    private static final int MAX_IMAGE_KEY_LENGTH = 500;
    private static final int MAX_ORIGINAL_NAME_LENGTH = 255;
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );


    @Column(name = "image_key", nullable = false, updatable = false, length = 500)
    private String imageKey;

    /** 사용자가 업로드한 원본 파일명 */
    @Column(name = "original_name", nullable = false, updatable = false, length = 255)
    private String originalName;

    /** S3 업로드 및 파일 형식 검증에 사용한 MIME 타입 */
    @Column(name = "content_type", nullable = false, updatable = false, length = 100)
    private String contentType;

    /** 업로드된 파일의 크기이며 단위는 byte다. */
    @Column(name = "file_size", nullable = false, updatable = false)
    private long fileSize;

    private Image(
            String imageKey,
            String originalName,
            String contentType,
            long fileSize
    ) {
        validateImageKey(imageKey);
        validateOriginalName(originalName);
        validateContentType(contentType);
        validateFileSize(fileSize);

        this.imageKey = imageKey;
        this.originalName = originalName;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }


    public static Image create(
            String imageKey,
            String originalName,
            String contentType,
            long fileSize
    ) {
        return new Image(imageKey, originalName, contentType, fileSize);
    }


    public void delete(String deletedBy) {
        if (isDeleted()) {
            return;
        }
        softDelete(deletedBy);
    }

    private static void validateImageKey(String imageKey) {
        if (imageKey == null || imageKey.isBlank() || imageKey.length() > MAX_IMAGE_KEY_LENGTH) {
            throw new BusinessException(ErrorCode.IMAGE_INVALID_KEY);
        }
    }

    private static void validateOriginalName(String originalName) {
        if (originalName == null
                || originalName.isBlank()
                || originalName.length() > MAX_ORIGINAL_NAME_LENGTH) {
            throw new BusinessException(ErrorCode.IMAGE_INVALID_ORIGINAL_NAME);
        }
    }

    private static void validateContentType(String contentType) {
        if (contentType == null || !SUPPORTED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.IMAGE_INVALID_CONTENT_TYPE);
        }
    }

    private static void validateFileSize(long fileSize) {
        if (fileSize <= 0 || fileSize > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.IMAGE_INVALID_FILE_SIZE);
        }
    }
}
