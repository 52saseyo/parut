package com.parut.product.productStock;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.product.application.authorization.stock.ProductStockAuthorizationChecker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

public class ProductStockAuthorizationCheckerTest {
    private final ProductStockAuthorizationChecker checker = new ProductStockAuthorizationChecker();

    @Test
    @DisplayName("ADMIN이면 소유자가 아니어도 통과한다")
    void requireOwnerOrAdmin_admin_passes() {
        assertThatCode(() -> checker.requireOwnerOrAdmin(UUID.randomUUID(), "ADMIN", UUID.randomUUID()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SELLER이고 소유자면 통과한다")
    void requireOwnerOrAdmin_sellerOwner_passes() {
        UUID id = UUID.randomUUID();
        assertThatCode(() -> checker.requireOwnerOrAdmin(id, "SELLER", id))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SELLER인데 소유자가 아니면 예외가 발생한다")
    void requireOwnerOrAdmin_sellerNotOwner_throws() {
        UUID sellerId = UUID.randomUUID();
        assertThatThrownBy(() -> checker.requireOwnerOrAdmin(UUID.randomUUID(), "SELLER", sellerId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_FORBIDDEN);
    }
    @Test
    @DisplayName("role이 SELLER/ADMIN이 아니면 예외가 발생한다")
    void requireOwnerOrAdmin_unknownRole_throws() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> checker.requireOwnerOrAdmin(id, "GUEST", id))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_FORBIDDEN);
    }

    @Test
    @DisplayName("isAdmin은 ADMIN일 때만 true를 반환한다")
    void isAdmin_returnsTrueOnlyForAdmin() {
        assertThat(checker.isAdmin("ADMIN")).isTrue();
        assertThat(checker.isAdmin("SELLER")).isFalse();
        assertThat(checker.isAdmin(null)).isFalse();
    }
}
