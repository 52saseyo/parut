package com.parut.product.image.domain.timeDealImage;

import com.parut.product.global.common.entity.DeletableEntity;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "p_time_deal_images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimeDealImage extends DeletableEntity {
    @Column(name = "time_deal_id", nullable = false, updatable = false)
    private UUID timeDealId;

    @Column(name = "image_id", nullable = false, updatable = false)
    private UUID imageId;

    @Column(name = "image_url", nullable = false, updatable = false, length = 1000)
    private String imageUrl;

    private TimeDealImage(
            UUID timeDealId,
            UUID imageId,
            String imageUrl
    ) {
        validateTimeDealId(timeDealId);
        validateImageId(imageId);
        validateImageUrl(imageUrl);

        this.timeDealId = timeDealId;
        this.imageId = imageId;
        this.imageUrl = imageUrl;
    }

    public static TimeDealImage create(
            UUID timeDealId,
            UUID imageId,
            String imageUrl
    ) {
        return new TimeDealImage(
                timeDealId,
                imageId,
                imageUrl
        );
    }

    public void delete(String deletedBy) {
        if (!isDeleted()) {
            softDelete(deletedBy);
        }
    }

    private static void validateTimeDealId(UUID timeDealId) {
        if (timeDealId == null) {
            throw new BusinessException(ErrorCode.TIME_DEAL_IMAGE_TIME_DEAL_ID_REQUIRED);
        }
    }

    private static void validateImageId(UUID imageId) {
        if (imageId == null) {
            throw new BusinessException(ErrorCode.TIME_DEAL_IMAGE_ID_REQUIRED);
        }
    }

    private static void validateImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new BusinessException(ErrorCode.IMAGE_INVALID_URL);
        }
    }
}
