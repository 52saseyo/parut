package com.parut.product.image.application.service;

import com.parut.product.global.dto.ImageQueryResult;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.image.domain.image.Image;
import com.parut.product.image.domain.timeDealImage.TimeDealImage;
import com.parut.product.image.infrastructure.persistence.TimeDealImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TimeDealImageService {
    private final ImageService imageService;
    private final TimeDealImageRepository timeDealImageRepository;


    @Transactional
    public void registerImage(UUID uploaderId, UUID timeDealId, UUID imageId){
        boolean alreadyExists =
                timeDealImageRepository.existsByTimeDealIdAndDeletedAtIsNull(timeDealId);

        if(alreadyExists){
            throw new BusinessException(ErrorCode.TIME_DEAL_IMAGE_ALREADY_EXISTS);
        }

        Image image = imageService.getOwnedImage(uploaderId, imageId);

        TimeDealImage timeDealImage = TimeDealImage.create(timeDealId, image.getId(), image.getImageUrl());

        timeDealImageRepository.save(timeDealImage);
    }


    @Transactional
    public void inheritProductImage(UUID timeDealId, UUID imageId, String imageUrl){
        if(timeDealImageRepository.existsByTimeDealIdAndDeletedAtIsNull(timeDealId)) {
            throw new BusinessException(ErrorCode.TIME_DEAL_IMAGE_ALREADY_EXISTS);
        }

        TimeDealImage timeDealImage = TimeDealImage.create(timeDealId, imageId, imageUrl);

        timeDealImageRepository.save(timeDealImage);
    }


    @Transactional(readOnly = true)
    public ImageQueryResult getImageInfo(UUID timeDealId){
        TimeDealImage image = timeDealImageRepository
                .findByTimeDealIdAndDeletedAtIsNull(timeDealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TIME_DEAL_IMAGE_NOT_FOUND));

        return new ImageQueryResult(image.getImageId(), image.getImageUrl());
    }

}
