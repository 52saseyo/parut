package com.parut.product.image.application.service;

import com.parut.product.global.dto.ImageQueryResult;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.image.domain.image.Image;
import com.parut.product.image.domain.timeDealImage.TimeDealImage;
import com.parut.product.image.infrastructure.persistence.TimeDealImageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TimeDealImageServiceTest {
    private static final UUID UPLOADER_ID = UUID.randomUUID();
    private static final UUID TIME_DEAL_ID = UUID.randomUUID();
    private static final UUID IMAGE_ID = UUID.randomUUID();
    private static final String IMAGE_URL = "https://example.com/image.jpg";

    @Mock ImageService imageService;
    @Mock TimeDealImageRepository timeDealImageRepository;
    @InjectMocks TimeDealImageService timeDealImageService;

    @Test
    void 소유한_이미지를_타임딜에_연결한다() {
        given(imageService.getOwnedImage(UPLOADER_ID, IMAGE_ID)).willReturn(image());

        timeDealImageService.registerImage(UPLOADER_ID, TIME_DEAL_ID, IMAGE_ID);

        ArgumentCaptor<TimeDealImage> captor = ArgumentCaptor.forClass(TimeDealImage.class);
        verify(timeDealImageRepository).save(captor.capture());
        assertThat(captor.getValue().getTimeDealId()).isEqualTo(TIME_DEAL_ID);
        assertThat(captor.getValue().getImageId()).isEqualTo(IMAGE_ID);
        assertThat(captor.getValue().getImageUrl()).isEqualTo(IMAGE_URL);
    }

    @Test
    void 타임딜에_활성_이미지가_있으면_추가_연결을_거부한다() {
        given(timeDealImageRepository.existsByTimeDealIdAndDeletedAtIsNull(TIME_DEAL_ID)).willReturn(true);

        assertBusinessException(
                () -> timeDealImageService.registerImage(UPLOADER_ID, TIME_DEAL_ID, IMAGE_ID),
                ErrorCode.TIME_DEAL_IMAGE_ALREADY_EXISTS
        );
        verify(imageService, never()).getOwnedImage(UPLOADER_ID, IMAGE_ID);
    }

    @Test
    void 타임딜_이미지_정보를_반환한다() {
        given(timeDealImageRepository.findByTimeDealIdAndDeletedAtIsNull(TIME_DEAL_ID))
                .willReturn(Optional.of(TimeDealImage.create(TIME_DEAL_ID, IMAGE_ID, IMAGE_URL)));

        ImageQueryResult result = timeDealImageService.getImageInfo(TIME_DEAL_ID);

        assertThat(result.imageId()).isEqualTo(IMAGE_ID);
        assertThat(result.imageUrl()).isEqualTo(IMAGE_URL);
    }

    @Test
    void 타임딜_이미지가_없으면_예외가_발생한다() {
        assertBusinessException(
                () -> timeDealImageService.getImageInfo(TIME_DEAL_ID),
                ErrorCode.TIME_DEAL_IMAGE_NOT_FOUND
        );
    }

    private Image image() {
        Image image = Image.create(
                UPLOADER_ID, "images/key.jpg", IMAGE_URL, "image.jpg", "image/jpeg", 1_024L
        );
        ReflectionTestUtils.setField(image, "id", IMAGE_ID);
        return image;
    }

    private void assertBusinessException(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(code));
    }
}
