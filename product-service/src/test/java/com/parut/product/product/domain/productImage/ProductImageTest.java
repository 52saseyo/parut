package com.parut.product.product.domain.productImage;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.product.domain.product.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductImageTest {

    private static Product newProduct() {
        return Product.create(
                UUID.randomUUID(),
                ProductCategory.FRUIT,
                "사과",
                "상품 설명",
                10_000L,
                AppearanceType.NORMAL,
                "국내산",
                LocalDate.of(2026, 9, 1),
                SaleUnit.BOX,
                BigDecimal.ONE
        );
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
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("상품 이미지의 상품, 키, 타입과 순서를 설정한다")
        void 생성_성공() {
            Product product = newProduct();

            ProductImage image = ProductImage.create(
                    product,
                    "products/1/detail.jpg",
                    ProductImageType.DETAIL,
                    1
            );

            assertThat(image.getProduct()).isSameAs(product);
            assertThat(image.getImageKey()).isEqualTo("products/1/detail.jpg");
            assertThat(image.getImageType()).isEqualTo(ProductImageType.DETAIL);
            assertThat(image.getSortOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("소속 상품이 없으면 생성할 수 없다")
        void 상품_null_생성_실패() {
            assertBusinessException(
                    () -> ProductImage.create(
                            null,
                            "products/1/detail.jpg",
                            ProductImageType.DETAIL,
                            1
                    ),
                    ErrorCode.PRODUCT_IMAGE_PRODUCT_REQUIRED
            );
        }

        @Test
        @DisplayName("이미지 키가 비어 있으면 생성할 수 없다")
        void 빈_이미지_키_생성_실패() {
            assertBusinessException(
                    () -> ProductImage.create(
                            newProduct(),
                            " ",
                            ProductImageType.DETAIL,
                            1
                    ),
                    ErrorCode.PRODUCT_IMAGE_INVALID_KEY
            );
        }

        @Test
        @DisplayName("이미지 타입이 없으면 생성할 수 없다")
        void 이미지_타입_null_생성_실패() {
            assertBusinessException(
                    () -> ProductImage.create(
                            newProduct(),
                            "products/1/detail.jpg",
                            null,
                            1
                    ),
                    ErrorCode.PRODUCT_IMAGE_INVALID_TYPE
            );
        }

        @Test
        @DisplayName("음수 노출 순서로 생성할 수 없다")
        void 음수_노출_순서_생성_실패() {
            assertBusinessException(
                    () -> ProductImage.create(
                            newProduct(),
                            "products/1/detail.jpg",
                            ProductImageType.DETAIL,
                            -1
                    ),
                    ErrorCode.PRODUCT_IMAGE_INVALID_SORT_ORDER
            );
        }
    }

    @Nested
    @DisplayName("소프트 삭제")
    class Delete {

        @Test
        @DisplayName("이미지를 삭제하면 삭제자와 삭제 시각이 기록된다")
        void 삭제_성공() {
            ProductImage image = ProductImage.create(
                    newProduct(),
                    "products/1/detail.jpg",
                    ProductImageType.DETAIL,
                    1
            );

            image.delete("tester");

            assertThat(image.isDeleted()).isTrue();
            assertThat(image.getDeletedBy()).isEqualTo("tester");
            assertThat(image.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("이미 삭제된 이미지를 다시 삭제해도 예외가 발생하지 않는다")
        void 중복_삭제는_아무_작업도_하지_않는다() {
            ProductImage image = ProductImage.create(
                    newProduct(),
                    "products/1/detail.jpg",
                    ProductImageType.DETAIL,
                    1
            );
            image.delete("first-user");

            assertThatCode(() -> image.delete("second-user"))
                    .doesNotThrowAnyException();
            assertThat(image.getDeletedBy()).isEqualTo("first-user");
        }
    }

    @Nested
    @DisplayName("상세 이미지 순서 변경")
    class ChangeDetailSortOrder {

        @Test
        @DisplayName("상세 이미지의 노출 순서를 변경한다")
        void 상세_이미지_순서_변경_성공() {
            ProductImage image = ProductImage.create(
                    newProduct(),
                    "products/1/detail.jpg",
                    ProductImageType.DETAIL,
                    1
            );

            image.changeDetailSortOrder(2);

            assertThat(image.getSortOrder()).isEqualTo(2);
            assertThat(image.getImageType()).isEqualTo(ProductImageType.DETAIL);
        }

        @Test
        @DisplayName("상세 이미지의 노출 순서는 1 이상이어야 한다")
        void 상세_이미지_순서_검증() {
            ProductImage image = ProductImage.create(
                    newProduct(),
                    "products/1/detail.jpg",
                    ProductImageType.DETAIL,
                    1
            );

            assertBusinessException(
                    () -> image.changeDetailSortOrder(0),
                    ErrorCode.PRODUCT_DETAIL_IMAGE_SORT_ORDER_INVALID
            );
        }

        @Test
        @DisplayName("대표 이미지의 순서를 상세 이미지 순서로 변경할 수 없다")
        void 대표_이미지_상세_순서_변경_실패() {
            ProductImage image = ProductImage.create(
                    newProduct(),
                    "products/1/main.jpg",
                    ProductImageType.MAIN,
                    0
            );

            assertBusinessException(
                    () -> image.changeDetailSortOrder(1),
                    ErrorCode.PRODUCT_IMAGE_INVALID_TYPE
            );
        }
    }

    @FunctionalInterface
    private interface ThrowingCallable {
        void call();
    }
}
