package com.parut.product.timedeal.domain.timedealpurchase;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeDealPurchaseTest {

    private static final Instant CREATED_AT = Instant.parse("2026-09-04T10:00:00Z");
    private static final Instant START_AT = Instant.parse("2026-09-04T11:00:00Z");
    private static final Instant END_AT = Instant.parse("2026-09-04T13:00:00Z");

    // 판매 기간(11:00~13:00) 안에서 선점하므로 만료 시각은 11:40이 된다.
    private static final Instant RESERVED_AT = Instant.parse("2026-09-04T11:30:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-09-04T11:40:00Z");
    private static final Instant BEFORE_EXPIRY = Instant.parse("2026-09-04T11:39:59Z");
    private static final Instant AFTER_EXPIRY = Instant.parse("2026-09-04T11:40:01Z");

    private static final int MAX_PURCHASE_QUANTITY = 5;

    private static TimeDeal deal() {
        return TimeDeal.create(
                UUID.randomUUID(), 10_000L, BigDecimal.valueOf(30),
                START_AT, END_AT, MAX_PURCHASE_QUANTITY, CREATED_AT);
    }

    private static TimeDealPurchase purchase() {
        return TimeDealPurchase.create(deal(), UUID.randomUUID(), UUID.randomUUID(), 2, RESERVED_AT);
    }

    private static TimeDealPurchase confirmedPurchase() {
        TimeDealPurchase purchase = purchase();
        purchase.confirm(BEFORE_EXPIRY);
        return purchase;
    }

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("생성 직후 상태는 RESERVED이다")
        void 생성_성공() {
            TimeDealPurchase purchase = purchase();

            assertThat(purchase.getStatus()).isEqualTo(TimeDealPurchaseStatus.RESERVED);
            assertThat(purchase.getQuantity()).isEqualTo(2);
            assertThat(purchase.getReservedAt()).isEqualTo(RESERVED_AT);
        }

        @Test
        @DisplayName("만료 시각은 입력값이 아니라 선점 시각 + 10분으로 계산된다")
        void 만료시각_계산() {
            TimeDealPurchase purchase = purchase();

            assertThat(purchase.getExpiresAt()).isEqualTo(EXPIRES_AT);
            assertThat(Duration.between(purchase.getReservedAt(), purchase.getExpiresAt()))
                    .isEqualTo(Duration.ofMinutes(10));
        }

        @Test
        @DisplayName("판매 기간을 벗어난 시각에는 선점할 수 없다")
        void 판매기간_밖() {
            Instant afterSaleEnd = Instant.parse("2026-09-04T13:00:01Z");

            assertThatThrownBy(() -> TimeDealPurchase.create(
                    deal(), UUID.randomUUID(), UUID.randomUUID(), 2, afterSaleEnd))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_SALE_PERIOD_INVALID);
        }

        @Test
        @DisplayName("구매 수량이 1개 미만이면 예외")
        void 수량_하한() {
            assertThatThrownBy(() -> TimeDealPurchase.create(
                    deal(), UUID.randomUUID(), UUID.randomUUID(), 0, RESERVED_AT))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_INVALID_PURCHASE_QUANTITY);
        }

        @Test
        @DisplayName("필수값이 null이면 예외")
        void 필수값_null() {
            assertThatThrownBy(() -> TimeDealPurchase.create(
                    deal(), null, UUID.randomUUID(), 2, RESERVED_AT))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }

        @Test
        @DisplayName("1인당 누적 제한은 생성 시점에 검증하지 않는다 — 상한을 넘는 수량도 생성 자체는 통과한다")
        void 누적_제한은_생성_책임이_아님() {
            TimeDealPurchase purchase = TimeDealPurchase.create(
                    deal(), UUID.randomUUID(), UUID.randomUUID(), MAX_PURCHASE_QUANTITY + 1, RESERVED_AT);

            assertThat(purchase.getQuantity()).isEqualTo(MAX_PURCHASE_QUANTITY + 1);
        }
    }

    @Nested
    @DisplayName("판매 확정")
    class Confirm {

        @Test
        @DisplayName("만료 전에는 확정할 수 있다")
        void 확정_성공() {
            TimeDealPurchase purchase = purchase();

            purchase.confirm(BEFORE_EXPIRY);

            assertThat(purchase.getStatus()).isEqualTo(TimeDealPurchaseStatus.CONFIRMED);
        }

        @Test
        @DisplayName("만료 시각 정각은 아직 확정할 수 있다")
        void 만료_경계_통과() {
            TimeDealPurchase purchase = purchase();

            purchase.confirm(EXPIRES_AT);

            assertThat(purchase.getStatus()).isEqualTo(TimeDealPurchaseStatus.CONFIRMED);
        }

        @Test
        @DisplayName("만료 시각을 넘기면 확정할 수 없다 — 배치가 늦어도 status만 믿지 않는다")
        void 만료_후_확정_불가() {
            TimeDealPurchase purchase = purchase();

            assertThatThrownBy(() -> purchase.confirm(AFTER_EXPIRY))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_RESERVATION_EXPIRED);
        }

        @Test
        @DisplayName("이미 확정된 건은 다시 확정할 수 없다")
        void 중복_확정_불가() {
            TimeDealPurchase purchase = confirmedPurchase();

            assertThatThrownBy(() -> purchase.confirm(BEFORE_EXPIRY))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_INVALID_STATUS_TRANSITION);
        }

        @Test
        @DisplayName("취소된 건은 확정할 수 없다")
        void 취소건_확정_불가() {
            TimeDealPurchase purchase = purchase();
            purchase.cancel(TimeDealPurchaseCancelReason.ORDER_CANCELED.name());

            assertThatThrownBy(() -> purchase.confirm(BEFORE_EXPIRY))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_INVALID_STATUS_TRANSITION);
        }

        @Test
        @DisplayName("기준 시각이 null이면 예외")
        void 기준시각_null() {
            TimeDealPurchase purchase = purchase();

            assertThatThrownBy(() -> purchase.confirm(null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Nested
    @DisplayName("취소")
    class Cancel {

        @Test
        @DisplayName("선점 상태에서 취소할 수 있다")
        void 선점_취소() {
            TimeDealPurchase purchase = purchase();

            purchase.cancel(TimeDealPurchaseCancelReason.PAYMENT_FAILED.name());

            assertThat(purchase.getStatus()).isEqualTo(TimeDealPurchaseStatus.CANCELLED);
            assertThat(purchase.getCancelReason())
                    .isEqualTo(TimeDealPurchaseCancelReason.PAYMENT_FAILED.name());
        }

        @Test
        @DisplayName("결제 완료된 건도 배송 시작 전까지는 취소할 수 있다")
        void 확정건_취소() {
            TimeDealPurchase purchase = confirmedPurchase();

            purchase.cancel(TimeDealPurchaseCancelReason.ORDER_CANCELED.name());

            assertThat(purchase.getStatus()).isEqualTo(TimeDealPurchaseStatus.CANCELLED);
        }

        @Test
        @DisplayName("이미 취소된 건은 다시 취소할 수 없다")
        void 중복_취소_불가() {
            TimeDealPurchase purchase = purchase();
            purchase.cancel(TimeDealPurchaseCancelReason.ORDER_CANCELED.name());

            assertThatThrownBy(() -> purchase.cancel(TimeDealPurchaseCancelReason.ORDER_CANCELED.name()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_INVALID_STATUS_TRANSITION);
        }

        @Test
        @DisplayName("사유는 생략할 수 있다")
        void 사유_null_허용() {
            TimeDealPurchase purchase = purchase();

            purchase.cancel(null);

            assertThat(purchase.getStatus()).isEqualTo(TimeDealPurchaseStatus.CANCELLED);
            assertThat(purchase.getCancelReason()).isNull();
        }

        @Test
        @DisplayName("사유 30자는 허용된다 — 컬럼 길이 경계")
        void 사유_길이_경계() {
            TimeDealPurchase purchase = purchase();

            purchase.cancel("가".repeat(30));

            assertThat(purchase.getCancelReason()).hasSize(30);
        }

        @Test
        @DisplayName("사유가 30자를 넘으면 예외 — DB에서 터지기 전에 도메인이 막는다")
        void 사유_길이_초과() {
            TimeDealPurchase purchase = purchase();

            assertThatThrownBy(() -> purchase.cancel("가".repeat(31)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_INVALID_CANCEL_REASON);
        }
    }

    @Nested
    @DisplayName("만료 처리")
    class Expire {

        @Test
        @DisplayName("만료된 선점은 취소 상태가 되고 사유가 기록된다")
        void 만료_성공() {
            TimeDealPurchase purchase = purchase();

            purchase.expire(AFTER_EXPIRY);

            assertThat(purchase.getStatus()).isEqualTo(TimeDealPurchaseStatus.CANCELLED);
            assertThat(purchase.getCancelReason())
                    .isEqualTo(TimeDealPurchaseCancelReason.RESERVATION_EXPIRED.name());
        }

        @Test
        @DisplayName("아직 만료되지 않았으면 만료 처리할 수 없다")
        void 만료_전_처리_불가() {
            TimeDealPurchase purchase = purchase();

            assertThatThrownBy(() -> purchase.expire(BEFORE_EXPIRY))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_INVALID_STATUS_TRANSITION);
        }

        @Test
        @DisplayName("이미 확정된 건은 시각이 지나도 만료 처리 대상이 아니다")
        void 확정건_만료_불가() {
            TimeDealPurchase purchase = confirmedPurchase();

            assertThatThrownBy(() -> purchase.expire(AFTER_EXPIRY))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_INVALID_STATUS_TRANSITION);
        }
    }

    @Nested
    @DisplayName("만료 여부 판정")
    class IsExpired {

        @Test
        @DisplayName("선점 상태에서 만료 시각이 지나면 만료다")
        void 만료됨() {
            assertThat(purchase().isExpired(AFTER_EXPIRY)).isTrue();
        }

        @Test
        @DisplayName("만료 시각 전이면 만료가 아니다")
        void 만료_안됨() {
            assertThat(purchase().isExpired(BEFORE_EXPIRY)).isFalse();
        }

        @Test
        @DisplayName("확정된 건은 시각이 지나도 만료가 아니다")
        void 확정건은_만료_아님() {
            assertThat(confirmedPurchase().isExpired(AFTER_EXPIRY)).isFalse();
        }

        @Test
        @DisplayName("기준 시각이 null이면 예외")
        void 기준시각_null() {
            TimeDealPurchase purchase = purchase();

            assertThatThrownBy(() -> purchase.isExpired(null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Nested
    @DisplayName("삭제")
    class SoftDelete {

        @Test
        @DisplayName("선점 상태의 구매 이력은 삭제할 수 없다")
        void 선점건_삭제_불가() {
            TimeDealPurchase purchase = purchase();

            assertThatThrownBy(() -> purchase.softDelete("tester"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_PURCHASE_DELETE_NOT_ALLOWED);
        }

        @Test
        @DisplayName("취소된 구매 이력도 삭제할 수 없다 — 정산·통계의 근거로 남긴다")
        void 취소건_삭제_불가() {
            TimeDealPurchase purchase = purchase();
            purchase.cancel(TimeDealPurchaseCancelReason.ORDER_CANCELED.name());

            assertThatThrownBy(() -> purchase.softDelete("tester"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_PURCHASE_DELETE_NOT_ALLOWED);
        }
    }
}