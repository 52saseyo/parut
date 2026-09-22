package com.parut.product.image.application.service;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.image.application.dto.LinkedImage;
import com.parut.product.image.domain.image.Image;
import com.parut.product.image.domain.productImage.ProductImage;
import com.parut.product.image.infrastructure.persistence.ProductImageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceTest {
    private static final UUID UPLOADER_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID IMAGE_ID = UUID.randomUUID();
    private static final String IMAGE_URL = "https://example.com/image.jpg";

    @Mock ImageService imageService;
    @Mock ProductImageRepository productImageRepository;
    @InjectMocks ProductImageService productImageService;

    @Test
    void 소유한_이미지를_상품에_연결한다() {
        given(imageService.getOwnedImage(UPLOADER_ID, IMAGE_ID)).willReturn(image());

        productImageService.registerImage(UPLOADER_ID, PRODUCT_ID, IMAGE_ID);

        ArgumentCaptor<ProductImage> captor = ArgumentCaptor.forClass(ProductImage.class);
        verify(productImageRepository).save(captor.capture());
        assertThat(captor.getValue().getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(captor.getValue().getImageId()).isEqualTo(IMAGE_ID);
        assertThat(captor.getValue().getImageUrl()).isEqualTo(IMAGE_URL);
    }

    @Test
    void 상품에_활성_이미지가_있으면_추가_연결을_거부한다() {
        given(productImageRepository.existsByProductIdAndDeletedAtIsNull(PRODUCT_ID)).willReturn(true);

        assertBusinessException(
                () -> productImageService.registerImage(UPLOADER_ID, PRODUCT_ID, IMAGE_ID),
                ErrorCode.PRODUCT_IMAGE_ALREADY_EXISTS
        );
        verify(imageService, never()).getOwnedImage(UPLOADER_ID, IMAGE_ID);
    }

    @Test
    void 상품_이미지_정보를_DTO로_반환한다() {
        given(productImageRepository.findByProductIdAndDeletedAtIsNull(PRODUCT_ID))
                .willReturn(Optional.of(ProductImage.create(PRODUCT_ID, IMAGE_ID, IMAGE_URL)));

        Optional<LinkedImage> result = productImageService.getImageInfo(PRODUCT_ID);

        assertThat(result).contains(new LinkedImage(IMAGE_ID, IMAGE_URL));
    }

    @Test
    void 여러_상품의_이미지_정보를_상품아이디별로_반환한다() {
        UUID secondProductId = UUID.randomUUID();
        UUID secondImageId = UUID.randomUUID();
        String secondImageUrl = "https://example.com/image2.jpg";
        List<UUID> productIds = List.of(PRODUCT_ID, secondProductId);
        given(productImageRepository.findAllByProductIdInAndDeletedAtIsNull(productIds))
                .willReturn(List.of(
                        ProductImage.create(PRODUCT_ID, IMAGE_ID, IMAGE_URL),
                        ProductImage.create(secondProductId, secondImageId, secondImageUrl)
                ));

        Map<UUID, LinkedImage> result = productImageService.getImageInfos(productIds);

        assertThat(result)
                .containsEntry(PRODUCT_ID, new LinkedImage(IMAGE_ID, IMAGE_URL))
                .containsEntry(secondProductId, new LinkedImage(secondImageId, secondImageUrl));
    }

    @Test
    void 상품아이디가_비어있으면_이미지를_조회하지_않는다() {
        assertThat(productImageService.getImageInfos(List.of())).isEmpty();

        verifyNoInteractions(productImageRepository);
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
