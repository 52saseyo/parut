package com.parut.product.image.application.service;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.image.application.dto.UploadedImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3ImageServiceTest {
    private static final UUID UPLOADER_ID = UUID.randomUUID();
    private static final String BUCKET = "test-bucket";
    private static final String BASE_URL = "https://test-bucket.s3.ap-northeast-2.amazonaws.com";
    private static final String IMAGE_KEY = "images/" + UPLOADER_ID + "/image.jpg";

    @Mock S3Client s3Client;
    @Mock S3Presigner s3Presigner;
    @InjectMocks S3ImageService s3ImageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(s3ImageService, "bucket", BUCKET);
        ReflectionTestUtils.setField(s3ImageService, "baseUrl", BASE_URL);
        ReflectionTestUtils.setField(s3ImageService, "expirationSeconds", 600L);
    }

    @Test
    void S3에_업로드된_객체의_메타데이터와_URL을_반환한다() {
        given(s3Client.headObject(any(HeadObjectRequest.class)))
                .willReturn(HeadObjectResponse.builder()
                        .contentType("image/jpeg")
                        .contentLength(1_024L)
                        .build());

        UploadedImage result = s3ImageService.checkS3UploadedImage(UPLOADER_ID, IMAGE_KEY);

        assertThat(result.imageKey()).isEqualTo(IMAGE_KEY);
        assertThat(result.imageUrl()).isEqualTo(BASE_URL + "/" + IMAGE_KEY);
        assertThat(result.contentType()).isEqualTo("image/jpeg");
        assertThat(result.fileSize()).isEqualTo(1_024L);
    }

    @Test
    void 다른_업로더의_이미지_키는_조회하지_않는다() {
        String otherKey = "images/" + UUID.randomUUID() + "/image.jpg";

        assertBusinessException(
                () -> s3ImageService.checkS3UploadedImage(UPLOADER_ID, otherKey),
                ErrorCode.IMAGE_INVALID_KEY
        );
        verify(s3Client, never()).headObject(any(HeadObjectRequest.class));
    }

    @Test
    void S3에_객체가_없으면_업로드_없음_예외가_발생한다() {
        given(s3Client.headObject(any(HeadObjectRequest.class)))
                .willThrow(S3Exception.builder().statusCode(404).build());

        assertBusinessException(
                () -> s3ImageService.checkS3UploadedImage(UPLOADER_ID, IMAGE_KEY),
                ErrorCode.IMAGE_UPLOAD_NOT_FOUND
        );
    }

    @Test
    void S3_객체의_콘텐츠_타입이_허용되지_않으면_거부한다() {
        given(s3Client.headObject(any(HeadObjectRequest.class)))
                .willReturn(HeadObjectResponse.builder()
                        .contentType("image/gif")
                        .contentLength(1_024L)
                        .build());

        assertBusinessException(
                () -> s3ImageService.checkS3UploadedImage(UPLOADER_ID, IMAGE_KEY),
                ErrorCode.IMAGE_INVALID_CONTENT_TYPE
        );
    }

    @Test
    void URL_발급_전에_파일_크기와_콘텐츠_타입을_검증한다() {
        assertBusinessException(
                () -> s3ImageService.createUploadUrl(UPLOADER_ID, "image/gif", 1_024L),
                ErrorCode.IMAGE_INVALID_CONTENT_TYPE
        );
        assertBusinessException(
                () -> s3ImageService.createUploadUrl(UPLOADER_ID, "image/jpeg", 10L * 1024 * 1024 + 1),
                ErrorCode.IMAGE_INVALID_FILE_SIZE
        );
        verify(s3Presigner, never()).presignPutObject(any(software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest.class));
    }

    private void assertBusinessException(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(code));
    }
}
