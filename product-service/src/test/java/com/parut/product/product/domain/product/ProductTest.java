package com.parut.product.product.domain.product;

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

class ProductTest {

    private static final String DELETED_BY = "tester";

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
    @DisplayName("상품 등록")
    class Create {

        @Test
        @DisplayName("상품 정보를 입력하면 DRAFT 상태로 등록된다")
        void 상품_등록_성공() {
            UUID sellerId = UUID.randomUUID();
            LocalDate harvestDate = LocalDate.of(2026, 9, 1);

            Product product = Product.create(
                    sellerId,
                    ProductCategory.FRUIT,
                    "사과",
                    "상품 설명",
                    10_000L,
                    AppearanceType.NORMAL,
                    "국내산",
                    harvestDate,
                    SaleUnit.BOX,
                    BigDecimal.valueOf(2)
            );

            assertThat(product.getSellerId()).isEqualTo(sellerId);
            assertThat(product.getCategory()).isEqualTo(ProductCategory.FRUIT);
            assertThat(product.getName()).isEqualTo("사과");
            assertThat(product.getDescription()).isEqualTo("상품 설명");
            assertThat(product.getPrice()).isEqualTo(10_000L);
            assertThat(product.getAppearanceType()).isEqualTo(AppearanceType.NORMAL);
            assertThat(product.getOrigin()).isEqualTo("국내산");
            assertThat(product.getHarvestDate()).isEqualTo(harvestDate);
            assertThat(product.getSaleUnit()).isEqualTo(SaleUnit.BOX);
            assertThat(product.getUnitQuantity()).isEqualByComparingTo("2");
            assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
        }

        @Test
        @DisplayName("판매자 ID가 없으면 상품을 등록할 수 없다")
        void 판매자_ID가_없는_상품_등록_실패() {
            assertBusinessException(
                    () -> Product.create(
                            null,
                            ProductCategory.FRUIT,
                            "사과",
                            "상품 설명",
                            10_000L,
                            AppearanceType.NORMAL,
                            "국내산",
                            LocalDate.of(2026, 9, 1),
                            SaleUnit.BOX,
                            BigDecimal.ONE
                    ),
                    ErrorCode.PRODUCT_INVALID_SELLER_ID
            );
        }

        @Test
        @DisplayName("상품명이 비어 있으면 상품을 등록할 수 없다")
        void 빈_상품명_등록_실패() {
            assertBusinessException(
                    () -> Product.create(
                            UUID.randomUUID(),
                            ProductCategory.FRUIT,
                            " ",
                            "상품 설명",
                            10_000L,
                            AppearanceType.NORMAL,
                            "국내산",
                            LocalDate.of(2026, 9, 1),
                            SaleUnit.BOX,
                            BigDecimal.ONE
                    ),
                    ErrorCode.PRODUCT_INVALID_NAME
            );
        }

        @Test
        @DisplayName("상품 가격이 음수이면 상품을 등록할 수 없다")
        void 음수_가격_상품_등록_실패() {
            assertBusinessException(
                    () -> Product.create(
                            UUID.randomUUID(),
                            ProductCategory.FRUIT,
                            "사과",
                            "상품 설명",
                            -1L,
                            AppearanceType.NORMAL,
                            "국내산",
                            LocalDate.of(2026, 9, 1),
                            SaleUnit.BOX,
                            BigDecimal.ONE
                    ),
                    ErrorCode.PRODUCT_INVALID_PRICE
            );
        }

        @Test
        @DisplayName("판매 단위 수량이 0이면 상품을 등록할 수 없다")
        void 판매_단위_수량_0_상품_등록_실패() {
            assertBusinessException(
                    () -> Product.create(
                            UUID.randomUUID(),
                            ProductCategory.FRUIT,
                            "사과",
                            "상품 설명",
                            10_000L,
                            AppearanceType.NORMAL,
                            "국내산",
                            LocalDate.of(2026, 9, 1),
                            SaleUnit.BOX,
                            BigDecimal.ZERO
                    ),
                    ErrorCode.PRODUCT_INVALID_UNIT_QUANTITY
            );
        }
    }

    @Nested
    @DisplayName("상품 수정")
    class Update {

        @Test
        @DisplayName("DRAFT 상태에서는 전달된 값만 수정한다")
        void 상품_부분_수정_성공() {
            Product product = newProduct();

            product.update(
                    ProductCategory.VEGETABLE,
                    "못난이 감자",
                    null,
                    8_000L,
                    AppearanceType.UGLY,
                    null,
                    null,
                    SaleUnit.KG,
                    BigDecimal.valueOf(3)
            );

            assertThat(product.getCategory()).isEqualTo(ProductCategory.VEGETABLE);
            assertThat(product.getName()).isEqualTo("못난이 감자");
            assertThat(product.getDescription()).isEqualTo("상품 설명");
            assertThat(product.getPrice()).isEqualTo(8_000L);
            assertThat(product.getAppearanceType()).isEqualTo(AppearanceType.UGLY);
            assertThat(product.getOrigin()).isEqualTo("국내산");
            assertThat(product.getHarvestDate()).isEqualTo(LocalDate.of(2026, 9, 1));
            assertThat(product.getSaleUnit()).isEqualTo(SaleUnit.KG);
            assertThat(product.getUnitQuantity()).isEqualByComparingTo("3");
        }

        @Test
        @DisplayName("모든 수정값이 null이면 기존 정보를 유지한다")
        void 전체_null이면_기존값_유지() {
            Product product = newProduct();

            product.update(null, null, null, null, null, null, null, null, null);

            assertThat(product.getCategory()).isEqualTo(ProductCategory.FRUIT);
            assertThat(product.getName()).isEqualTo("사과");
            assertThat(product.getDescription()).isEqualTo("상품 설명");
            assertThat(product.getPrice()).isEqualTo(10_000L);
            assertThat(product.getAppearanceType()).isEqualTo(AppearanceType.NORMAL);
            assertThat(product.getOrigin()).isEqualTo("국내산");
            assertThat(product.getHarvestDate()).isEqualTo(LocalDate.of(2026, 9, 1));
            assertThat(product.getSaleUnit()).isEqualTo(SaleUnit.BOX);
            assertThat(product.getUnitQuantity()).isEqualByComparingTo("1");
        }

        @Test
        @DisplayName("잘못된 가격으로 수정하면 기존 가격을 유지하고 예외가 발생한다")
        void 잘못된_가격_수정_실패() {
            Product product = newProduct();

            assertBusinessException(
                    () -> product.update(
                            null, null, null, -1L, null,
                            null, null, null, null
                    ),
                    ErrorCode.PRODUCT_INVALID_PRICE
            );
            assertThat(product.getPrice()).isEqualTo(10_000L);
        }

        @Test
        @DisplayName("ON_SALE 상태에서는 상품 정보를 수정할 수 없다")
        void 판매_중_상품_수정_실패() {
            Product product = newProduct();
            product.addImage(UUID.randomUUID());
            product.startSale();

            assertBusinessException(
                    () -> product.update(
                            null, "수정 상품명", null, null, null,
                            null, null, null, null
                    ),
                    ErrorCode.PRODUCT_NOT_MODIFIABLE
            );
            assertThat(product.getName()).isEqualTo("사과");
        }

        @Test
        @DisplayName("SUSPENDED 상태에서는 상품 정보를 수정할 수 있다")
        void 판매_중지_상품_수정_성공() {
            Product product = newProduct();
            product.addImage(UUID.randomUUID());
            product.startSale();
            product.suspend();

            product.update(
                    null, "수정 상품명", null, null, null,
                    null, null, null, null
            );

            assertThat(product.getName()).isEqualTo("수정 상품명");
            assertThat(product.getStatus()).isEqualTo(ProductStatus.SUSPENDED);
        }

        @Test
        @DisplayName("삭제된 상품은 수정할 수 없다")
        void 삭제된_상품_수정_실패() {
            Product product = newProduct();
            product.delete(DELETED_BY);

            assertBusinessException(
                    () -> product.update(
                            null, "수정 상품명", null, null, null,
                            null, null, null, null
                    ),
                    ErrorCode.PRODUCT_NOT_MODIFIABLE
            );
        }
    }

    @Nested
    @DisplayName("상품 삭제")
    class Delete {

        @Test
        @DisplayName("상품을 삭제하면 DELETED 상태로 전환하고 소프트 삭제한다")
        void 상품_삭제_성공() {
            Product product = newProduct();

            product.delete(DELETED_BY);

            assertThat(product.getStatus()).isEqualTo(ProductStatus.DELETED);
            assertThat(product.isDeleted()).isTrue();
            assertThat(product.getDeletedAt()).isNotNull();
            assertThat(product.getDeletedBy()).isEqualTo(DELETED_BY);
        }

        @Test
        @DisplayName("이미 삭제된 상품은 다시 삭제할 수 없다")
        void 상품_중복_삭제_실패() {
            Product product = newProduct();
            product.delete(DELETED_BY);

            assertBusinessException(
                    () -> product.delete("another-user"),
                    ErrorCode.PRODUCT_ALREADY_DELETED
            );
        }
    }

    @Nested
    @DisplayName("상품 이미지")
    class ProductImage {

        @Test
        @DisplayName("이미지가 없는 상품에 최초 이미지를 등록한다")
        void 최초_이미지_등록_성공() {
            Product product = newProduct();
            UUID imageId = UUID.randomUUID();

            product.addImage(imageId);

            assertThat(product.getImageId()).isEqualTo(imageId);
        }

        @Test
        @DisplayName("이미지 ID가 없으면 최초 이미지를 등록할 수 없다")
        void 이미지_ID가_없는_최초_등록_실패() {
            Product product = newProduct();

            assertBusinessException(
                    () -> product.addImage(null),
                    ErrorCode.PRODUCT_IMAGE_ID_REQUIRED
            );
            assertThat(product.getImageId()).isNull();
        }

        @Test
        @DisplayName("이미지가 이미 등록된 상품에는 최초 등록을 다시 할 수 없다")
        void 이미지_중복_등록_실패() {
            Product product = newProduct();
            UUID firstImageId = UUID.randomUUID();
            product.addImage(firstImageId);

            assertBusinessException(
                    () -> product.addImage(UUID.randomUUID()),
                    ErrorCode.PRODUCT_IMAGE_ALREADY_EXISTS
            );
            assertThat(product.getImageId()).isEqualTo(firstImageId);
        }

        @Test
        @DisplayName("기존 이미지를 변경하면 이전 이미지 ID를 반환한다")
        void 이미지_변경_성공() {
            Product product = newProduct();
            UUID previousImageId = UUID.randomUUID();
            UUID newImageId = UUID.randomUUID();
            product.addImage(previousImageId);

            UUID result = product.changeImage(newImageId);

            assertThat(result).isEqualTo(previousImageId);
            assertThat(product.getImageId()).isEqualTo(newImageId);
        }

        @Test
        @DisplayName("기존 이미지가 없으면 이미지를 변경할 수 없다")
        void 기존_이미지가_없는_변경_실패() {
            Product product = newProduct();

            assertBusinessException(
                    () -> product.changeImage(UUID.randomUUID()),
                    ErrorCode.PRODUCT_IMAGE_NOT_FOUND
            );
            assertThat(product.getImageId()).isNull();
        }

        @Test
        @DisplayName("이미지를 제거하면 기존 이미지 ID를 반환하고 연결을 해제한다")
        void 이미지_제거_성공() {
            Product product = newProduct();
            UUID imageId = UUID.randomUUID();
            product.addImage(imageId);

            UUID result = product.removeImage();

            assertThat(result).isEqualTo(imageId);
            assertThat(product.getImageId()).isNull();
        }

        @Test
        @DisplayName("기존 이미지가 없으면 이미지를 제거할 수 없다")
        void 기존_이미지가_없는_제거_실패() {
            Product product = newProduct();

            assertBusinessException(
                    product::removeImage,
                    ErrorCode.PRODUCT_IMAGE_NOT_FOUND
            );
        }

        @Test
        @DisplayName("판매 중인 상품의 이미지는 변경할 수 없다")
        void 판매_중_이미지_변경_실패() {
            Product product = newProduct();
            UUID imageId = UUID.randomUUID();
            product.addImage(imageId);
            product.startSale();

            assertBusinessException(
                    () -> product.changeImage(UUID.randomUUID()),
                    ErrorCode.PRODUCT_NOT_MODIFIABLE
            );
            assertThat(product.getImageId()).isEqualTo(imageId);
        }
    }

    @Nested
    @DisplayName("상품 판매 상태 전환")
    class SaleStatusTransition {

        @Test
        @DisplayName("이미지가 없으면 판매를 시작할 수 없다")
        void 이미지가_없는_판매_시작_실패() {
            Product product = newProduct();

            assertBusinessException(
                    product::startSale,
                    ErrorCode.PRODUCT_IMAGE_REQUIRED
            );
            assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
        }

        @Test
        @DisplayName("이미지가 있으면 판매를 시작할 수 있다")
        void 이미지가_있는_판매_시작_성공() {
            Product product = newProduct();
            product.addImage(UUID.randomUUID());

            assertThatCode(product::startSale).doesNotThrowAnyException();
            assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        }
    }


    @FunctionalInterface
    private interface ThrowingCallable {
        void call();
    }
}
