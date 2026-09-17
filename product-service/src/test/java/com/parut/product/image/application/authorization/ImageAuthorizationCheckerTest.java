package com.parut.product.image.application.authorization;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageAuthorizationCheckerTest {

    private final ImageAuthorizationChecker checker = new ImageAuthorizationChecker();

    @Test
    void 판매자는_이미지를_업로드할_수_있다() {
        assertThatCode(() -> checker.requireUploaderRole("SELLER"))
                .doesNotThrowAnyException();
    }

    @Test
    void 관리자는_이미지를_업로드할_수_있다() {
        assertThatCode(() -> checker.requireUploaderRole("ADMIN"))
                .doesNotThrowAnyException();
    }

    @Test
    void 구매자는_이미지를_업로드할_수_없다() {
        assertAccessDenied(() -> checker.requireUploaderRole("CUSTOMER"));
    }

    @Test
    void 역할이_없으면_이미지를_업로드할_수_없다() {
        assertAccessDenied(() -> checker.requireUploaderRole(null));
    }

    private void assertAccessDenied(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.IMAGE_ACCESS_DENIED)
                );
    }
}
