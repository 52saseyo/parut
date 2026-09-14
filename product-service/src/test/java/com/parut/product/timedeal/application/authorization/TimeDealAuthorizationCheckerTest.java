package com.parut.product.timedeal.application.authorization;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeDealAuthorizationCheckerTest {

    private final TimeDealAuthorizationChecker checker = new TimeDealAuthorizationChecker();
    private final UUID owner = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();

    @Test
    void 판매자_본인은_자신의_타임딜을_관리할_수_있다() {
        assertThatCode(() -> checker.requireSellerOwner(owner, "SELLER", owner)).doesNotThrowAnyException();
        assertThatCode(() -> checker.requireSellerOwnerOrAdmin(owner, "SELLER", owner)).doesNotThrowAnyException();
    }

    @Test
    void 다른_판매자는_타임딜을_관리할_수_없다() {
        assertAccessDenied(() -> checker.requireSellerOwner(other, "SELLER", owner));
        assertAccessDenied(() -> checker.requireSellerOwnerOrAdmin(other, "SELLER", owner));
    }

    @Test
    void 관리자_예외는_관리자를_허용하는_검사에만_적용된다() {
        assertThatCode(() -> checker.requireSellerOwnerOrAdmin(other, "ADMIN", owner)).doesNotThrowAnyException();
        assertThatCode(() -> checker.requireAdmin("ADMIN")).doesNotThrowAnyException();
        assertAccessDenied(() -> checker.requireSellerOwner(other, "ADMIN", owner));
        assertAccessDenied(() -> checker.requireSellerOwner(owner, "ADMIN", owner));
        assertAccessDenied(() -> checker.requireAdmin("SELLER"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "SELLER"})
    void 생성은_판매자와_관리자_역할을_허용한다(String role) {
        assertThatCode(() -> checker.requireSellerOrAdmin(role)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"CUSTOMER", "UNKNOWN", "admin", "seller", " ADMIN", "SELLER ", " "})
    void 소유자라도_허용되지_않은_역할은_거절한다(String role) {
        assertAccessDenied(() -> checker.requireSeller(role));
        assertAccessDenied(() -> checker.requireSellerOrAdmin(role));
        assertAccessDenied(() -> checker.requireSellerOwner(owner, role, owner));
        assertAccessDenied(() -> checker.requireSellerOwnerOrAdmin(owner, role, owner));
        assertAccessDenied(() -> checker.requireAdmin(role));
    }

    @Test
    void 요청자_ID가_없으면_판매자_소유권_검사를_거절한다() {
        assertAccessDenied(() -> checker.requireOwner(null, owner));
        assertAccessDenied(() -> checker.requireSellerOwner(null, "SELLER", owner));
        assertAccessDenied(() -> checker.requireSellerOwnerOrAdmin(null, "SELLER", owner));
    }

    @Test
    void 관리자는_소유권_ID를_검사하지_않는다() {
        assertThatCode(() -> checker.requireSellerOwnerOrAdmin(null, "ADMIN", null))
                .doesNotThrowAnyException();
    }

    @Test
    void 소유권_단독_검사는_ID_일치만_확인한다() {
        assertThatCode(() -> checker.requireOwner(owner, owner)).doesNotThrowAnyException();
        assertAccessDenied(() -> checker.requireOwner(other, owner));
        assertAccessDenied(() -> checker.requireOwner(owner, null));
        assertAccessDenied(() -> checker.requireOwner(null, null));
    }

    @Test
    void 판매자_역할_검사는_판매자만_허용한다() {
        assertThatCode(() -> checker.requireSeller("SELLER")).doesNotThrowAnyException();
        assertAccessDenied(() -> checker.requireSeller("ADMIN"));
    }

    @Test
    void 소유자_ID가_없으면_판매자_소유권_검사를_거절한다() {
        assertAccessDenied(() -> checker.requireSellerOwner(owner, "SELLER", null));
        assertAccessDenied(() -> checker.requireSellerOwner(null, "SELLER", null));
        assertAccessDenied(() -> checker.requireSellerOwnerOrAdmin(owner, "SELLER", null));
    }

    private void assertAccessDenied(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TIME_DEAL_ACCESS_DENIED);
    }
}
