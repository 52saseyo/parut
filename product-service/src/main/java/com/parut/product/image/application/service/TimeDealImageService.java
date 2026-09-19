package com.parut.product.image.application.service;

import com.parut.product.global.dto.ImageQueryResult;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.image.domain.image.Image;
import com.parut.product.image.domain.timeDealImage.TimeDealImage;
import com.parut.product.image.infrastructure.persistence.ImageRepository;
import com.parut.product.image.infrastructure.persistence.TimeDealImageRepository;
import com.parut.product.timedeal.application.authorization.TimeDealAuthorizationChecker;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TimeDealImageService {
    private final ImageService imageService;
    private final TimeDealImageRepository timeDealImageRepository;
    private final ImageRepository imageRepository;
    private final TimeDealRepository timeDealRepository;
    private final TimeDealAuthorizationChecker authorizationChecker;

    @Transactional
    public void registerImage(UUID requesterId, String requesterRole, UUID timeDealId, UUID imageId){
        var timeDeal = timeDealRepository.findById(timeDealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TIME_DEAL_NOT_FOUND));
        authorizationChecker.requireSellerOwnerOrAdmin(requesterId, requesterRole, timeDeal.getSellerId());
        registerImage(requesterId, timeDealId, imageId);
    }


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
    public void copyFromProductImage(UUID timeDealId, UUID imageId){
        if(timeDealImageRepository.existsByTimeDealIdAndDeletedAtIsNull(timeDealId)) {
            throw new BusinessException(ErrorCode.TIME_DEAL_IMAGE_ALREADY_EXISTS);
        }
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));

        TimeDealImage timeDealImage = TimeDealImage.create(timeDealId, image.getId(), image.getImageUrl());

        timeDealImageRepository.save(timeDealImage);
    }


    @Transactional(readOnly = true)
    public Optional<ImageQueryResult> getImageInfo(UUID timeDealId){
       return timeDealImageRepository
               .findByTimeDealIdAndDeletedAtIsNull(timeDealId)
               .map(image -> new ImageQueryResult(
                       image.getImageId(),
                       image.getImageUrl()
               ));
    }

}
