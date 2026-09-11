package com.parut.product.image.domain.image;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageTest {

    private static final String IMAGE_KEY = "products/550e8400-e29b-41d4-a716-446655440000.jpg";
    private static final String ORIGINAL_NAME = "apple.jpg";
    private static final String CONTENT_TYPE = "image/jpeg";
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

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
        @DisplayName("이미지 키와 파일 메타데이터로 이미지를 생성한다")
        void 이미지_생성_성공() {
            Image image = Image.create(
                    IMAGE_KEY,
                    ORIGINAL_NAME,
                    CONTENT_TYPE,
                    1_024L
            );

            assertThat(image.getImageKey()).isEqualTo(IMAGE_KEY);
            assertThat(image.getOriginalName()).isEqualTo(ORIGINAL_NAME);
            assertThat(image.getContentType()).isEqualTo(CONTENT_TYPE);
            assertThat(image.getFileSize()).isEqualTo(1_024L);
            assertThat(image.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("이미지 키가 비어 있으면 생성할 수 없다")
        void 빈_이미지_키_생성_실패() {
            assertBusinessException(
                    () -> Image.create(" ", ORIGINAL_NAME, CONTENT_TYPE, 1_024L),
                    ErrorCode.IMAGE_INVALID_KEY
            );
            assertBusinessException(
                    () -> Image.create(null, ORIGINAL_NAME, CONTENT_TYPE, 1_024L),
                    ErrorCode.IMAGE_INVALID_KEY
            );
        }

        @Test
        @DisplayName("이미지 키가 500자를 초과하면 생성할 수 없다")
        void 너무_긴_이미지_키_생성_실패() {
            assertBusinessException(
                    () -> Image.create("a".repeat(501), ORIGINAL_NAME, CONTENT_TYPE, 1_024L),
                    ErrorCode.IMAGE_INVALID_KEY
            );
        }

        @Test
        @DisplayName("원본 파일명이 비어 있으면 생성할 수 없다")
        void 빈_원본_파일명_생성_실패() {
            assertBusinessException(
                    () -> Image.create(IMAGE_KEY, " ", CONTENT_TYPE, 1_024L),
                    ErrorCode.IMAGE_INVALID_ORIGINAL_NAME
            );
            assertBusinessException(
                    () -> Image.create(IMAGE_KEY, null, CONTENT_TYPE, 1_024L),
                    ErrorCode.IMAGE_INVALID_ORIGINAL_NAME
            );
        }

        @Test
        @DisplayName("원본 파일명이 255자를 초과하면 생성할 수 없다")
        void 너무_긴_원본_파일명_생성_실패() {
            assertBusinessException(
                    () -> Image.create(IMAGE_KEY, "a".repeat(256), CONTENT_TYPE, 1_024L),
                    ErrorCode.IMAGE_INVALID_ORIGINAL_NAME
            );
        }

        @Test
        @DisplayName("지원하지 않는 콘텐츠 타입이면 생성할 수 없다")
        void 지원하지_않는_콘텐츠_타입_생성_실패() {
            assertBusinessException(
                    () -> Image.create(IMAGE_KEY, ORIGINAL_NAME, "image/gif", 1_024L),
                    ErrorCode.IMAGE_INVALID_CONTENT_TYPE
            );
            assertBusinessException(
                    () -> Image.create(IMAGE_KEY, ORIGINAL_NAME, null, 1_024L),
                    ErrorCode.IMAGE_INVALID_CONTENT_TYPE
            );
        }

        @Test
        @DisplayName("지원하는 모든 콘텐츠 타입으로 생성할 수 있다")
        void 지원하는_콘텐츠_타입_생성_성공() {
            assertThat(Image.create(IMAGE_KEY + ".jpeg", ORIGINAL_NAME, "image/jpeg", 1_024L))
                    .extracting(Image::getContentType)
                    .isEqualTo("image/jpeg");
            assertThat(Image.create(IMAGE_KEY + ".png", "apple.png", "image/png", 1_024L))
                    .extracting(Image::getContentType)
                    .isEqualTo("image/png");
            assertThat(Image.create(IMAGE_KEY + ".webp", "apple.webp", "image/webp", 1_024L))
                    .extracting(Image::getContentType)
                    .isEqualTo("image/webp");
        }

        @Test
        @DisplayName("파일 크기가 0이면 생성할 수 없다")
        void 빈_파일_생성_실패() {
            assertBusinessException(
                    () -> Image.create(IMAGE_KEY, ORIGINAL_NAME, CONTENT_TYPE, 0L),
                    ErrorCode.IMAGE_INVALID_FILE_SIZE
            );
        }

        @Test
        @DisplayName("파일 크기가 10MB를 초과하면 생성할 수 없다")
        void 최대_파일_크기_초과_생성_실패() {
            assertBusinessException(
                    () -> Image.create(IMAGE_KEY, ORIGINAL_NAME, CONTENT_TYPE, MAX_FILE_SIZE + 1),
                    ErrorCode.IMAGE_INVALID_FILE_SIZE
            );
        }

        @Test
        @DisplayName("파일 크기가 정확히 10MB이면 생성할 수 있다")
        void 최대_파일_크기_경계값_생성_성공() {
            Image image = Image.create(
                    IMAGE_KEY,
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
            Image image = Image.create(IMAGE_KEY, ORIGINAL_NAME, CONTENT_TYPE, 1_024L);

            image.delete("seller-id");

            assertThat(image.isDeleted()).isTrue();
            assertThat(image.getDeletedAt()).isNotNull();
            assertThat(image.getDeletedBy()).isEqualTo("seller-id");
        }

        @Test
        @DisplayName("이미 삭제된 이미지를 다시 삭제해도 최초 삭제 정보를 유지한다")
        void 이미지_중복_삭제는_동일한_상태를_유지한다() {
            Image image = Image.create(IMAGE_KEY, ORIGINAL_NAME, CONTENT_TYPE, 1_024L);
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
