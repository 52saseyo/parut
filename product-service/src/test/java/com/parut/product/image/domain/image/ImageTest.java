package com.parut.product.image.domain.image;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageTest {

    private static final UUID UPLOADER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String IMAGE_KEY = "images/550e8400-e29b-41d4-a716-446655440000/apple.jpg";
    private static final String IMAGE_URL = "https://example.com/" + IMAGE_KEY;
    private static final String ORIGINAL_NAME = "apple.jpg";
    private static final String CONTENT_TYPE = "image/jpeg";
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private static Image createImage(String imageKey, String originalName, String contentType, long fileSize) {
        return Image.create(UPLOADER_ID, imageKey, IMAGE_URL, originalName, contentType, fileSize);
    }

    private static void assertBusinessException(
            ThrowingCallable callable,
            ErrorCode expectedErrorCode
    ) {
        assertThatThrownBy(callable::call)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(expectedErrorCode);
    }

    @Nested
    @DisplayName("이미지 생성")
    class Create {

        @Test
        @DisplayName("업로더, 이미지 키, URL과 파일 메타데이터로 이미지를 생성한다")
        void 이미지_생성_성공() {
            Image image = Image.create(
                    UPLOADER_ID,
                    IMAGE_KEY,
                    IMAGE_URL,
                    ORIGINAL_NAME,
                    CONTENT_TYPE,
                    1_024L
            );

            assertThat(image.getUploaderId()).isEqualTo(UPLOADER_ID);
            assertThat(image.getImageKey()).isEqualTo(IMAGE_KEY);
            assertThat(image.getImageUrl()).isEqualTo(IMAGE_URL);
            assertThat(image.getOriginalName()).isEqualTo(ORIGINAL_NAME);
            assertThat(image.getContentType()).isEqualTo(CONTENT_TYPE);
            assertThat(image.getFileSize()).isEqualTo(1_024L);
            assertThat(image.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("업로더 ID가 없으면 생성할 수 없다")
        void 업로더_ID가_없는_이미지_생성_실패() {
            assertBusinessException(
                    () -> Image.create(null, IMAGE_KEY, IMAGE_URL, ORIGINAL_NAME, CONTENT_TYPE, 1_024L),
                    ErrorCode.IMAGE_INVALID_UPLOADER_ID
            );
        }

        @Test
        @DisplayName("이미지 URL이 비어 있거나 1000자를 초과하면 생성할 수 없다")
        void 잘못된_이미지_URL_생성_실패() {
            assertBusinessException(
                    () -> Image.create(UPLOADER_ID, IMAGE_KEY, " ", ORIGINAL_NAME, CONTENT_TYPE, 1_024L),
                    ErrorCode.IMAGE_INVALID_URL
            );
            assertBusinessException(
                    () -> Image.create(UPLOADER_ID, IMAGE_KEY, null, ORIGINAL_NAME, CONTENT_TYPE, 1_024L),
                    ErrorCode.IMAGE_INVALID_URL
            );
            assertBusinessException(
                    () -> Image.create(UPLOADER_ID, IMAGE_KEY, "a".repeat(1001), ORIGINAL_NAME, CONTENT_TYPE, 1_024L),
                    ErrorCode.IMAGE_INVALID_URL
            );
        }

        @Test
        @DisplayName("이미지 키가 비어 있으면 생성할 수 없다")
        void 빈_이미지_키_생성_실패() {
            assertBusinessException(
                    () -> createImage(" ", ORIGINAL_NAME, CONTENT_TYPE, 1_024L),
                    ErrorCode.IMAGE_INVALID_KEY
            );
            assertBusinessException(
                    () -> createImage(null, ORIGINAL_NAME, CONTENT_TYPE, 1_024L),
                    ErrorCode.IMAGE_INVALID_KEY
            );
        }

        @Test
        @DisplayName("이미지 키가 500자를 초과하면 생성할 수 없다")
        void 너무_긴_이미지_키_생성_실패() {
            assertBusinessException(
                    () -> createImage("a".repeat(501), ORIGINAL_NAME, CONTENT_TYPE, 1_024L),
                    ErrorCode.IMAGE_INVALID_KEY
            );
        }

        @Test
        @DisplayName("원본 파일명이 비어 있으면 생성할 수 없다")
        void 빈_원본_파일명_생성_실패() {
            assertBusinessException(
                    () -> createImage(IMAGE_KEY, " ", CONTENT_TYPE, 1_024L),
                    ErrorCode.IMAGE_INVALID_ORIGINAL_NAME
            );
            assertBusinessException(
                    () -> createImage(IMAGE_KEY, null, CONTENT_TYPE, 1_024L),
                    ErrorCode.IMAGE_INVALID_ORIGINAL_NAME
            );
        }

        @Test
        @DisplayName("원본 파일명이 255자를 초과하면 생성할 수 없다")
        void 너무_긴_원본_파일명_생성_실패() {
            assertBusinessException(
                    () -> createImage(IMAGE_KEY, "a".repeat(256), CONTENT_TYPE, 1_024L),
                    ErrorCode.IMAGE_INVALID_ORIGINAL_NAME
            );
        }

        @Test
        @DisplayName("지원하지 않는 콘텐츠 타입이면 생성할 수 없다")
        void 지원하지_않는_콘텐츠_타입_생성_실패() {
            assertBusinessException(
                    () -> createImage(IMAGE_KEY, ORIGINAL_NAME, "image/gif", 1_024L),
                    ErrorCode.IMAGE_INVALID_CONTENT_TYPE
            );
            assertBusinessException(
                    () -> createImage(IMAGE_KEY, ORIGINAL_NAME, null, 1_024L),
                    ErrorCode.IMAGE_INVALID_CONTENT_TYPE
            );
        }

        @Test
        @DisplayName("지원하는 모든 콘텐츠 타입으로 생성할 수 있다")
        void 지원하는_콘텐츠_타입_생성_성공() {
            assertThat(createImage(IMAGE_KEY + ".jpeg", ORIGINAL_NAME, "image/jpeg", 1_024L))
                    .extracting(Image::getContentType)
                    .isEqualTo("image/jpeg");
            assertThat(createImage(IMAGE_KEY + ".png", "apple.png", "image/png", 1_024L))
                    .extracting(Image::getContentType)
                    .isEqualTo("image/png");
            assertThat(createImage(IMAGE_KEY + ".webp", "apple.webp", "image/webp", 1_024L))
                    .extracting(Image::getContentType)
                    .isEqualTo("image/webp");
        }

        @Test
        @DisplayName("파일 크기가 0이면 생성할 수 없다")
        void 빈_파일_생성_실패() {
            assertBusinessException(
                    () -> createImage(IMAGE_KEY, ORIGINAL_NAME, CONTENT_TYPE, 0L),
                    ErrorCode.IMAGE_INVALID_FILE_SIZE
            );
        }

        @Test
        @DisplayName("파일 크기가 10MB를 초과하면 생성할 수 없다")
        void 최대_파일_크기_초과_생성_실패() {
            assertBusinessException(
                    () -> createImage(IMAGE_KEY, ORIGINAL_NAME, CONTENT_TYPE, MAX_FILE_SIZE + 1),
                    ErrorCode.IMAGE_INVALID_FILE_SIZE
            );
        }

        @Test
        @DisplayName("파일 크기가 정확히 10MB이면 생성할 수 있다")
        void 최대_파일_크기_경계값_생성_성공() {
            Image image = Image.create(
                    UPLOADER_ID,
                    IMAGE_KEY,
                    IMAGE_URL,
                    ORIGINAL_NAME,
                    CONTENT_TYPE,
                    MAX_FILE_SIZE
            );

            assertThat(image.getFileSize()).isEqualTo(MAX_FILE_SIZE);
        }
    }

    @Nested
    @DisplayName("이미지 삭제")
    class Delete {

        @Test
        @DisplayName("이미지를 삭제하면 삭제 일시와 삭제자를 기록한다")
        void 이미지_논리_삭제_성공() {
            Image image = createImage(IMAGE_KEY, ORIGINAL_NAME, CONTENT_TYPE, 1_024L);

            image.delete("seller-id");

            assertThat(image.isDeleted()).isTrue();
            assertThat(image.getDeletedAt()).isNotNull();
            assertThat(image.getDeletedBy()).isEqualTo("seller-id");
        }

        @Test
        @DisplayName("이미 삭제된 이미지를 다시 삭제해도 최초 삭제 정보를 유지한다")
        void 이미지_중복_삭제는_동일한_상태를_유지한다() {
            Image image = createImage(IMAGE_KEY, ORIGINAL_NAME, CONTENT_TYPE, 1_024L);
            image.delete("first-seller");
            var firstDeletedAt = image.getDeletedAt();

            image.delete("second-seller");

            assertThat(image.getDeletedAt()).isEqualTo(firstDeletedAt);
            assertThat(image.getDeletedBy()).isEqualTo("first-seller");
        }
    }

    @FunctionalInterface
    private interface ThrowingCallable {
        void call();
    }
}
