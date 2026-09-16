package com.parut.product.image.application.service;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.image.application.dto.ImageUploadUrlResult;
import com.parut.product.image.application.dto.UploadedImage;
import com.parut.product.image.domain.image.Image;
import com.parut.product.image.infrastructure.persistence.ImageRepository;
import com.parut.product.image.presentation.dto.request.ImageUploadCompleteRequest;
import com.parut.product.image.presentation.dto.request.ImageUploadUrlRequest;
import com.parut.product.image.presentation.dto.response.ImageResponse;
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
class ImageServiceTest {

    private static final UUID UPLOADER_ID = UUID.randomUUID();
    private static final UUID IMAGE_ID = UUID.randomUUID();
    private static final String IMAGE_KEY = "images/" + UPLOADER_ID + "/image.jpg";
    private static final String IMAGE_URL = "https://example.com/" + IMAGE_KEY;

    @Mock ImageRepository imageRepository;
    @Mock S3ImageService s3ImageService;
    @InjectMocks ImageService imageService;

    @Test
    void 업로드_URL_발급을_S3_서비스에_위임한다() {
        ImageUploadUrlRequest request = new ImageUploadUrlRequest("image/jpeg", 1_024L);
        ImageUploadUrlResult expected = new ImageUploadUrlResult(IMAGE_KEY, "https://upload.example.com");
        given(s3ImageService.createUploadUrl(UPLOADER_ID, "image/jpeg", 1_024L)).willReturn(expected);

        assertThat(imageService.createUploadUrl(UPLOADER_ID, request)).isSameAs(expected);
    }

    @Test
    void S3_업로드를_확인하고_이미지_정보를_저장한다() {
        ImageUploadCompleteRequest request = new ImageUploadCompleteRequest(IMAGE_KEY, "apple.jpg");
        given(s3ImageService.checkS3UploadedImage(UPLOADER_ID, IMAGE_KEY))
                .willReturn(new UploadedImage(IMAGE_KEY, IMAGE_URL, "image/jpeg", 1_024L));
        given(imageRepository.save(org.mockito.ArgumentMatchers.any(Image.class)))
                .willAnswer(invocation -> {
                    Image image = invocation.getArgument(0);
                    ReflectionTestUtils.setField(image, "id", IMAGE_ID);
                    return image;
                });

        ImageResponse response = imageService.completeUpload(UPLOADER_ID, request);

        assertThat(response.imageId()).isEqualTo(IMAGE_ID);
        assertThat(response.imageUrl()).isEqualTo(IMAGE_URL);
        ArgumentCaptor<Image> captor = ArgumentCaptor.forClass(Image.class);
        verify(imageRepository).save(captor.capture());
        assertThat(captor.getValue().getUploaderId()).isEqualTo(UPLOADER_ID);
        assertThat(captor.getValue().getOriginalName()).isEqualTo("apple.jpg");
    }

    @Test
    void 이미_등록된_이미지_키는_다시_저장하지_않는다() {
        given(imageRepository.existsByImageKey(IMAGE_KEY)).willReturn(true);

        assertBusinessException(
                () -> imageService.completeUpload(
                        UPLOADER_ID, new ImageUploadCompleteRequest(IMAGE_KEY, "apple.jpg")
                ),
                ErrorCode.IMAGE_ALREADY_EXISTS
        );
        verify(s3ImageService, never()).checkS3UploadedImage(UPLOADER_ID, IMAGE_KEY);
    }

    @Test
    void 업로더가_소유한_이미지를_조회한다() {
        Image image = image();
        given(imageRepository.findByIdAndUploaderIdAndDeletedAtIsNull(IMAGE_ID, UPLOADER_ID))
                .willReturn(Optional.of(image));

        assertThat(imageService.getOwnedImage(UPLOADER_ID, IMAGE_ID)).isSameAs(image);
    }

    @Test
    void 소유한_이미지가_없으면_예외가_발생한다() {
        assertBusinessException(
                () -> imageService.getOwnedImage(UPLOADER_ID, IMAGE_ID),
                ErrorCode.IMAGE_NOT_FOUND
        );
    }

    private Image image() {
        return Image.create(UPLOADER_ID, IMAGE_KEY, IMAGE_URL, "apple.jpg", "image/jpeg", 1_024L);
    }

    private void assertBusinessException(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(code));
    }
}
