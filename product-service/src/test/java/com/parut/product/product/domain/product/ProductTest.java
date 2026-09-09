package com.parut.product.product.domain.product;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.product.domain.product.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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

    private static UUID assignId(ProductImage image) {
        UUID id = UUID.randomUUID();
        ReflectionTestUtils.setField(image, "id", id);
        return id;
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
            assertThat(product.getActiveProductImages()).isEmpty();
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
            product.addMainImage("products/1/main.jpg");
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
            product.addMainImage("products/1/main.jpg");
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
    @DisplayName("대표 이미지 등록")
    class AddMainImage {

        @Test
        @DisplayName("대표 이미지는 MAIN 타입과 노출 순서 0으로 등록된다")
        void 대표_이미지_등록_성공() {
            Product product = newProduct();

            ProductImage mainImage = product.addMainImage("products/1/main.jpg");

            assertThat(mainImage.getProduct()).isSameAs(product);
            assertThat(mainImage.getImageKey()).isEqualTo("products/1/main.jpg");
            assertThat(mainImage.getImageType()).isEqualTo(ProductImageType.MAIN);
            assertThat(mainImage.getSortOrder()).isZero();
            assertThat(product.getActiveProductImages()).containsExactly(mainImage);
        }

        @Test
        @DisplayName("활성 대표 이미지가 이미 있으면 추가할 수 없다")
        void 대표_이미지_중복_등록_실패() {
            Product product = newProduct();
            product.addMainImage("products/1/main-a.jpg");

            assertBusinessException(
                    () -> product.addMainImage("products/1/main-b.jpg"),
                    ErrorCode.PRODUCT_MAIN_IMAGE_ALREADY_EXISTS
            );
        }

        @Test
        @DisplayName("상세 이미지가 5장 있어도 대표 이미지 한 장을 추가할 수 있다")
        void 대표_이미지는_상세_이미지_제한에_포함되지_않는다() {
            Product product = newProduct();
            for (int index = 1; index <= 5; index++) {
                product.addDetailImage("products/1/detail-" + index + ".jpg");
            }

            ProductImage mainImage = product.addMainImage("products/1/main.jpg");

            assertThat(mainImage.getImageType()).isEqualTo(ProductImageType.MAIN);
            assertThat(product.getActiveProductImages()).hasSize(6);
        }
    }

    @Nested
    @DisplayName("상세 이미지 등록")
    class AddDetailImage {

        @Test
        @DisplayName("상세 이미지 순서는 1부터 서버가 순차적으로 부여한다")
        void 상세_이미지_순서_자동_부여() {
            Product product = newProduct();

            ProductImage first = product.addDetailImage("products/1/detail-a.jpg");
            ProductImage second = product.addDetailImage("products/1/detail-b.jpg");
            ProductImage third = product.addDetailImage("products/1/detail-c.jpg");

            assertThat(first.getImageType()).isEqualTo(ProductImageType.DETAIL);
            assertThat(first.getSortOrder()).isEqualTo(1);
            assertThat(second.getSortOrder()).isEqualTo(2);
            assertThat(third.getSortOrder()).isEqualTo(3);
        }

        @Test
        @DisplayName("상세 이미지는 최대 5장까지 등록할 수 있다")
        void 상세_이미지_개수_제한() {
            Product product = newProduct();
            for (int index = 1; index <= 5; index++) {
                product.addDetailImage("products/1/detail-" + index + ".jpg");
            }

            assertBusinessException(
                    () -> product.addDetailImage("products/1/detail-6.jpg"),
                    ErrorCode.PRODUCT_DETAIL_IMAGE_LIMIT_EXCEEDED
            );
        }

        @Test
        @DisplayName("대표 이미지와 상세 이미지는 같은 활성 이미지 키를 사용할 수 없다")
        void 활성_이미지_키_중복_실패() {
            Product product = newProduct();
            product.addMainImage("products/1/same.jpg");

            assertBusinessException(
                    () -> product.addDetailImage("products/1/same.jpg"),
                    ErrorCode.PRODUCT_IMAGE_ALREADY_EXISTS
            );
        }

        @Test
        @DisplayName("빈 이미지 키는 등록할 수 없다")
        void 빈_이미지_키_등록_실패() {
            Product product = newProduct();

            assertBusinessException(
                    () -> product.addDetailImage(" "),
                    ErrorCode.PRODUCT_IMAGE_INVALID_KEY
            );
        }
    }

    @Nested
    @DisplayName("이미지 변경 가능 상태")
    class ImageModifiableStatus {

        @Test
        @DisplayName("ON_SALE 상태에서는 이미지를 추가할 수 없다")
        void 판매_중_이미지_추가_실패() {
            Product product = newProduct();
            product.addMainImage("products/1/main.jpg");
            product.startSale();

            assertBusinessException(
                    () -> product.addDetailImage("products/1/detail.jpg"),
                    ErrorCode.PRODUCT_IMAGE_NOT_MODIFIABLE
            );
        }

        @Test
        @DisplayName("SUSPENDED 상태에서는 이미지를 추가할 수 있다")
        void 판매_중지_이미지_추가_성공() {
            Product product = newProduct();
            product.addMainImage("products/1/main.jpg");
            product.startSale();
            product.suspend();

            ProductImage detailImage =
                    product.addDetailImage("products/1/detail.jpg");

            assertThat(detailImage.getImageType()).isEqualTo(ProductImageType.DETAIL);
            assertThat(detailImage.getSortOrder()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("이미지 삭제")
    class RemoveImage {

        @Test
        @DisplayName("상세 이미지를 삭제하면 뒤 이미지의 순서를 한 칸씩 당긴다")
        void 상세_이미지_삭제_후_순서_압축() {
            Product product = newProduct();
            ProductImage first = product.addDetailImage("products/1/detail-a.jpg");
            ProductImage second = product.addDetailImage("products/1/detail-b.jpg");
            ProductImage third = product.addDetailImage("products/1/detail-c.jpg");
            assignId(first);
            UUID secondId = assignId(second);
            assignId(third);

            String deletedImageKey =
                    product.removeProductImage(secondId, DELETED_BY);

            assertThat(deletedImageKey).isEqualTo("products/1/detail-b.jpg");
            assertThat(second.isDeleted()).isTrue();
            assertThat(second.getDeletedBy()).isEqualTo(DELETED_BY);
            assertThat(first.getSortOrder()).isEqualTo(1);
            assertThat(third.getSortOrder()).isEqualTo(2);
            assertThat(product.getActiveProductImages()).containsExactly(first, third);
        }


        @Test
        @DisplayName("상품에 없는 이미지 ID는 삭제할 수 없다")
        void 존재하지_않는_이미지_삭제_실패() {
            Product product = newProduct();

            assertBusinessException(
                    () -> product.removeProductImage(UUID.randomUUID(), DELETED_BY),
                    ErrorCode.PRODUCT_IMAGE_NOT_FOUND
            );
        }

        @Test
        @DisplayName("상품을 삭제하면 모든 활성 이미지도 소프트 삭제된다")
        void 상품_삭제_시_이미지_함께_삭제() {
            Product product = newProduct();
            ProductImage mainImage = product.addMainImage("products/1/main.jpg");
            ProductImage detailImage = product.addDetailImage("products/1/detail.jpg");

            product.delete(DELETED_BY);

            assertThat(product.getStatus()).isEqualTo(ProductStatus.DELETED);
            assertThat(product.isDeleted()).isTrue();
            assertThat(mainImage.isDeleted()).isTrue();
            assertThat(detailImage.isDeleted()).isTrue();
            assertThat(product.getActiveProductImages()).isEmpty();
        }
    }

    @Nested
    @DisplayName("판매 상태 전환")
    class SaleStatusTransition {

        @Test
        @DisplayName("대표 이미지가 없으면 판매를 시작할 수 없다")
        void 대표_이미지_없는_판매_시작_실패() {
            Product product = newProduct();
            product.addDetailImage("products/1/detail.jpg");

            assertBusinessException(
                    product::startSale,
                    ErrorCode.PRODUCT_MAIN_IMAGE_REQUIRED
            );
            assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
        }

        @Test
        @DisplayName("대표 이미지가 있으면 판매를 시작할 수 있다")
        void 대표_이미지_있는_판매_시작_성공() {
            Product product = newProduct();
            product.addMainImage("products/1/main.jpg");

            assertThatCode(product::startSale).doesNotThrowAnyException();
            assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        }

        @Test
        @DisplayName("품절 상품은 재입고 후 판매 중 상태로 전환할 수 있다")
        void 품절_상품_재입고_후_판매_재개_성공() {
            Product product = newProduct();
            product.addMainImage("products/1/main.jpg");
            product.startSale();
            product.soldOut();

            assertThatCode(product::resumeSaleAfterRestock).doesNotThrowAnyException();
            assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        }

        @Test
        @DisplayName("품절 상태가 아니면 재입고에 따른 판매 재개를 할 수 없다")
        void 품절_상태가_아닌_상품_재입고_후_판매_재개_실패() {
            Product product = newProduct();
            product.addMainImage("products/1/main.jpg");

            assertBusinessException(
                    product::resumeSaleAfterRestock,
                    ErrorCode.PRODUCT_STATUS_TRANSITION_NOT_ALLOWED
            );
            assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
        }
    }

    @FunctionalInterface
    private interface ThrowingCallable {
        void call();
    }
}
