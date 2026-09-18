package com.parut.product.product.application.authorization.product;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductAuthorizationCheckerTest {

    private final ProductAuthorizationChecker checker = new ProductAuthorizationChecker();

    @Test
    void 판매자_역할은_판매자_작업을_수행할_수_있다() {
        assertThatCode(() -> checker.requireSeller("SELLER"))
                .doesNotThrowAnyException();
    }

    @Test
    void 판매자가_아니면_판매자_작업을_수행할_수_없다() {
        assertAccessDenied(() -> checker.requireSeller("CUSTOMER"));
        assertAccessDenied(() -> checker.requireSeller(null));
    }

    @Test
    void 판매자와_관리자는_허용된_작업을_수행할_수_있다() {
        assertThatCode(() -> checker.requireSellerOrAdmin("SELLER"))
                .doesNotThrowAnyException();
        assertThatCode(() -> checker.requireSellerOrAdmin("ADMIN"))
                .doesNotThrowAnyException();
    }

    @Test
    void 상품_소유_판매자는_본인_상품을_관리할_수_있다() {
        UUID sellerId = UUID.randomUUID();

        assertThatCode(() -> checker.requireSellerOwner(sellerId, "SELLER", sellerId))
                .doesNotThrowAnyException();
    }

    @Test
    void 다른_판매자의_상품은_관리할_수_없다() {
        assertAccessDenied(() -> checker.requireSellerOwner(
                UUID.randomUUID(),
                "SELLER",
                UUID.randomUUID()
        ));
    }

    @Test
    void 관리자는_상품_소유자가_아니어도_관리할_수_있다() {
        assertThatCode(() -> checker.requireSellerOwnerOrAdmin(
                UUID.randomUUID(),
                "ADMIN",
                UUID.randomUUID()
        )).doesNotThrowAnyException();
    }

    private void assertAccessDenied(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.PRODUCT_ACCESS_DENIED)
                );
    }
}
