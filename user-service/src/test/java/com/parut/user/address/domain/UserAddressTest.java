package com.parut.user.address.domain;

import com.parut.user.global.exception.BusinessException;
import com.parut.user.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserAddressTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private UserAddress newAddress(boolean isDefault) {
        return new UserAddress(
                USER_ID,
                "집",
                "홍길동",
                "010-1234-5678",
                "06236",
                "서울특별시 강남구 테헤란로 123",
                "101동 101호",
                isDefault,
                "parut"
        );
    }

    @Test
    @DisplayName("필수 식별자와 문자열이 없으면 배송지를 생성할 수 없다")
    void rejectMissingRequiredValues() {
        assertThatThrownBy(() -> new UserAddress(
                USER_ID, " ", "홍길동", "010-1234-5678", "06236", "서울특별시 강남구", null, false, "parut"
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

        assertThatThrownBy(() -> new UserAddress(
                null, "집", "홍길동", "010-1234-5678", "06236", "서울특별시 강남구", null, false, "parut"
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

        assertThatThrownBy(() -> new UserAddress(
                USER_ID, "집", "홍길동", "010-1234-5678", "06236", "서울특별시 강남구", null, false, " "
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("부분 수정은 생략한 값을 유지하고 빈 상세 주소는 제거한다")
    void updatePartially() {
        UserAddress address = newAddress(false);

        address.update(null, "김파릇", null, null, null, null, "parut");

        assertThat(address.getRecipientName()).isEqualTo("김파릇");
        assertThat(address.getAddressName()).isEqualTo("집");
        assertThat(address.getZipCode()).isEqualTo("06236");
        assertThat(address.getAddressDetail()).isEqualTo("101동 101호");
        assertThat(address.getUpdatedBy()).isEqualTo("parut");
        assertThat(address.getUpdatedAt()).isNotNull();

        address.update(null, null, null, null, null, "", "parut");
        assertThat(address.getAddressDetail()).isNull();
    }

}
