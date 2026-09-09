package com.parut.product.timedeal.domain.common;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import com.parut.product.timedeal.domain.timedeal.TimeDealProductGrade;
import com.parut.product.timedeal.domain.timedeal.TimeDealStatus;
import com.parut.product.timedeal.domain.timedealpurchase.TimeDealPurchase;
import com.parut.product.timedeal.domain.timedealpurchase.TimeDealPurchaseCancelReason;
import com.parut.product.timedeal.domain.timedealpurchase.TimeDealPurchaseStatus;
import com.parut.product.timedeal.domain.timedealstock.TimeDealStock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// NOTE: Spring 컨텍스트와 DB 없이 도는 순수 단위 테스트다. TimeDealPolicy는 Repository를 갖지 않으므로
// new로 생성해 쓴다(@Component는 컨테이너만 읽는 메타데이터라 무관하다).
class TimeDealPolicyTest {

    private static final Instant CREATED_AT = Instant.parse("2026-09-04T10:00:00Z");
    private static final Instant HARVESTED_AT = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant START_AT = Instant.parse("2026-09-04T11:00:00Z");
    private static final Instant END_AT = Instant.parse("2026-09-04T12:00:00Z");
    private static final Instant IN_WINDOW = Instant.parse("2026-09-04T11:30:00Z");
    private static final Instant AFTER_END = Instant.parse("2026-09-04T12:00:01Z");
    // 선점 TTL 10분이 지난 시각. 판매 기간(12:00) 안이므로 "딜은 열려 있는데 선점만 만료된" 상태를 만든다.
    private static final Instant AFTER_RESERVATION_TTL = IN_WINDOW.plus(Duration.ofMinutes(11));

    private static final int INITIAL_QUANTITY = 100;
    private static final int MAX_PURCHASE_QUANTITY = 10;

    private final TimeDealPolicy timeDealPolicy = new TimeDealPolicy();

    private static TimeDeal scheduledTimeDeal(int maxPurchaseQuantity) {
        TimeDeal timeDeal = TimeDeal.create(
                UUID.randomUUID(), UUID.randomUUID(), null,
                "산지직송 사과 5kg", null, TimeDealProductGrade.NORMAL, "경북 안동", HARVESTED_AT,
                10_000L, BigDecimal.valueOf(30),
                START_AT, END_AT, maxPurchaseQuantity, CREATED_AT);
        // NOTE: 저장 전에는 id가 null이라 짝 검증(validateBelongsToTimeDeal)이 통과할 수 없다.
        ReflectionTestUtils.setField(timeDeal, "id", UUID.randomUUID());
        return timeDeal;
    }

    private static TimeDeal activeTimeDeal() {
        TimeDeal timeDeal = scheduledTimeDeal(MAX_PURCHASE_QUANTITY);
        timeDeal.activate(IN_WINDOW);
        return timeDeal;
    }

    private static TimeDealStock stockOf(TimeDeal timeDeal, int initialQuantity) {
        return TimeDealStock.create(timeDeal.getId(), initialQuantity, Math.min(10, initialQuantity));
    }

    private static int totalQuantity(TimeDealStock stock) {
        return stock.getAvailableQuantity() + stock.getReservedQuantity() + stock.getSoldQuantity();
    }


    @Nested
    @DisplayName("구매 예약")
    class Reserve {

        @Test
        @DisplayName("구매 이력이 RESERVED로 생성되고 재고가 선점된다")
        void 예약_성공() {
            TimeDeal timeDeal = activeTimeDeal();
            TimeDealStock stock = stockOf(timeDeal, INITIAL_QUANTITY);

            TimeDealPurchase purchase = timeDealPolicy.reserve(
                    timeDeal, stock, UUID.randomUUID(), UUID.randomUUID(), 5, 0, IN_WINDOW);

            assertThat(purchase.getStatus()).isEqualTo(TimeDealPurchaseStatus.RESERVED);
            assertThat(purchase.getTimeDealId()).isEqualTo(timeDeal.getId());
            assertThat(purchase.getReservedAt()).isEqualTo(IN_WINDOW);
            assertThat(stock.getAvailableQuantity()).isEqualTo(95);
            assertThat(stock.getReservedQuantity()).isEqualTo(5);
            assertThat(totalQuantity(stock)).isEqualTo(INITIAL_QUANTITY);
        }

        @Test
        @DisplayName("마지막 재고를 예약하면 타임딜이 ENDED로 조기 종료된다")
        void 소진_조기종료() {
            TimeDeal timeDeal = activeTimeDeal();
            TimeDealStock stock = stockOf(timeDeal, 5);

            timeDealPolicy.reserve(
                    timeDeal, stock, UUID.randomUUID(), UUID.randomUUID(), 5, 0, IN_WINDOW);

            assertThat(stock.isDepleted()).isTrue();
            assertThat(timeDeal.getStatus()).isEqualTo(TimeDealStatus.ENDED);
        }

        @Test
        @DisplayName("재고가 남아 있으면 타임딜 상태는 ACTIVE를 유지한다")
        void 미소진_상태유지() {
            TimeDeal timeDeal = activeTimeDeal();
            TimeDealStock stock = stockOf(timeDeal, 6);

            timeDealPolicy.reserve(
                    timeDeal, stock, UUID.randomUUID(), UUID.randomUUID(), 5, 0, IN_WINDOW);

            assertThat(timeDeal.getStatus()).isEqualTo(TimeDealStatus.ACTIVE);
        }

        @Test
        @DisplayName("1인당 누적 수량을 초과하면 예외이고, 재고는 건드려지지 않는다")
        void 누적한도_초과() {
            TimeDeal timeDeal = activeTimeDeal();
            TimeDealStock stock = stockOf(timeDeal, INITIAL_QUANTITY);

            assertThatThrownBy(() -> timeDealPolicy.reserve(
                    timeDeal, stock, UUID.randomUUID(), UUID.randomUUID(), 5, 6, IN_WINDOW))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_EXCEEDS_MAX_PURCHASE_QUANTITY);

            assertThat(stock.getAvailableQuantity()).isEqualTo(INITIAL_QUANTITY);
            assertThat(stock.getReservedQuantity()).isZero();
        }

        @Test
        @DisplayName("종료된 타임딜에 한도 초과 수량으로 요청하면 한도 초과가 아니라 판매 불가가 보고된다")
        void 검증_우선순위() {
            TimeDeal timeDeal = activeTimeDeal();
            TimeDealStock stock = stockOf(timeDeal, INITIAL_QUANTITY);
            timeDeal.end();

            assertThatThrownBy(() -> timeDealPolicy.reserve(
                    timeDeal, stock, UUID.randomUUID(), UUID.randomUUID(), 99, 0, IN_WINDOW))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_NOT_ACTIVE);
        }

        @Test
        @DisplayName("재고가 부족하면 예외")
        void 재고부족() {
            TimeDeal timeDeal = activeTimeDeal();
            TimeDealStock stock = stockOf(timeDeal, 3);

            assertThatThrownBy(() -> timeDealPolicy.reserve(
                    timeDeal, stock, UUID.randomUUID(), UUID.randomUUID(), 5, 0, IN_WINDOW))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_STOCK_INSUFFICIENT);
        }

        @Test
        @DisplayName("다른 타임딜의 재고가 넘어오면 예외")
        void 짝_불일치() {
            TimeDeal timeDeal = activeTimeDeal();
            TimeDealStock otherStock = stockOf(activeTimeDeal(), INITIAL_QUANTITY);

            assertThatThrownBy(() -> timeDealPolicy.reserve(
                    timeDeal, otherStock, UUID.randomUUID(), UUID.randomUUID(), 5, 0, IN_WINDOW))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_STOCK_MISMATCH);
        }

        @Test
        @DisplayName("재고가 null이면 예외 — NPE가 아니라 400으로 걸러진다")
        void 재고_null() {
            TimeDeal timeDeal = activeTimeDeal();

            assertThatThrownBy(() -> timeDealPolicy.reserve(
                    timeDeal, null, UUID.randomUUID(), UUID.randomUUID(), 5, 0, IN_WINDOW))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }
    }


    @Nested
    @DisplayName("판매 확정")
    class ConfirmSale {

        @Test
        @DisplayName("확정되면 선점 수량이 판매 수량으로 이동하고 총합은 보존된다")
        void 확정_성공() {
            TimeDeal timeDeal = activeTimeDeal();
            TimeDealStock stock = stockOf(timeDeal, INITIAL_QUANTITY);
            TimeDealPurchase purchase = timeDealPolicy.reserve(
                    timeDeal, stock, UUID.randomUUID(), UUID.randomUUID(), 5, 0, IN_WINDOW);

            TimeDealPurchaseConfirmResult result =
                    timeDealPolicy.confirmSale(purchase, stock, IN_WINDOW);

            assertThat(result).isEqualTo(TimeDealPurchaseConfirmResult.CONFIRMED);
            assertThat(purchase.getStatus()).isEqualTo(TimeDealPurchaseStatus.CONFIRMED);
            assertThat(stock.getReservedQuantity()).isZero();
            assertThat(stock.getSoldQuantity()).isEqualTo(5);
            assertThat(totalQuantity(stock)).isEqualTo(INITIAL_QUANTITY);
        }

        @Test
        @DisplayName("선점이 만료됐으면 예외 없이 CANCELLED를 반환하고 구매 이력과 재고를 정리한다")
        void 만료_선감지() {
            TimeDeal timeDeal = activeTimeDeal();
            TimeDealStock stock = stockOf(timeDeal, INITIAL_QUANTITY);
            TimeDealPurchase purchase = timeDealPolicy.reserve(
                    timeDeal, stock, UUID.randomUUID(), UUID.randomUUID(), 5, 0, IN_WINDOW);

            TimeDealPurchaseConfirmResult result =
                    timeDealPolicy.confirmSale(purchase, stock, AFTER_RESERVATION_TTL);

            assertThat(result).isEqualTo(TimeDealPurchaseConfirmResult.CANCELLED);
            assertThat(purchase.getStatus()).isEqualTo(TimeDealPurchaseStatus.CANCELLED);
            assertThat(purchase.getCancelReason())
                    .isEqualTo(TimeDealPurchaseCancelReason.RESERVATION_EXPIRED.name());
            assertThat(stock.getAvailableQuantity()).isEqualTo(INITIAL_QUANTITY);
            assertThat(stock.getReservedQuantity()).isZero();
            assertThat(stock.getSoldQuantity()).isZero();
            assertThat(totalQuantity(stock)).isEqualTo(INITIAL_QUANTITY);
        }

        @Test
        @DisplayName("이미 확정된 건을 다시 확정하면 예외 없이 CONFIRMED를 반환하고 재고가 이중 이동하지 않는다")
        void 확정_멱등() {
            TimeDeal timeDeal = activeTimeDeal();
            TimeDealStock stock = stockOf(timeDeal, INITIAL_QUANTITY);
            TimeDealPurchase purchase = timeDealPolicy.reserve(
                    timeDeal, stock, UUID.randomUUID(), UUID.randomUUID(), 5, 0, IN_WINDOW);
            timeDealPolicy.confirmSale(purchase, stock, IN_WINDOW);

            TimeDealPurchaseConfirmResult result =
                    timeDealPolicy.confirmSale(purchase, stock, IN_WINDOW);

            assertThat(result).isEqualTo(TimeDealPurchaseConfirmResult.CONFIRMED);
            assertThat(purchase.getStatus()).isEqualTo(TimeDealPurchaseStatus.CONFIRMED);
            assertThat(stock.getReservedQuantity()).isZero();
            assertThat(stock.getSoldQuantity()).isEqualTo(5);
            assertThat(totalQuantity(stock)).isEqualTo(INITIAL_QUANTITY);
        }

        @Test
        @DisplayName("선점 TTL이 지난 뒤 확정이 또 들어와도 확정 상태는 그대로다 — 만료 분기를 타지 않는다")
        void 확정_멱등_TTL경과후() {
            TimeDeal timeDeal = activeTimeDeal();
            TimeDealStock stock = stockOf(timeDeal, INITIAL_QUANTITY);
            TimeDealPurchase purchase = timeDealPolicy.reserve(
                    timeDeal, stock, UUID.randomUUID(), UUID.randomUUID(), 5, 0, IN_WINDOW);
            timeDealPolicy.confirmSale(purchase, stock, IN_WINDOW);

            TimeDealPurchaseConfirmResult result =
                    timeDealPolicy.confirmSale(purchase, stock, AFTER_RESERVATION_TTL);

            assertThat(result).isEqualTo(TimeDealPurchaseConfirmResult.CONFIRMED);
            assertThat(purchase.getStatus()).isEqualTo(TimeDealPurchaseStatus.CONFIRMED);
            assertThat(stock.getSoldQuantity()).isEqualTo(5);
            assertThat(totalQuantity(stock)).isEqualTo(INITIAL_QUANTITY);
        }

        @Test
        @DisplayName("이미 취소된 건에 확정이 오면 예외 — 만료가 아닌 사유로 취소됐을 수 있어 멱등 대상이 아니다")
        void 취소된건_확정() {
            TimeDeal timeDeal = activeTimeDeal();
            TimeDealStock stock = stockOf(timeDeal, INITIAL_QUANTITY);
            TimeDealPurchase purchase = timeDealPolicy.reserve(
                    timeDeal, stock, UUID.randomUUID(), UUID.randomUUID(), 5, 0, IN_WINDOW);
            timeDealPolicy.cancelPurchase(
                    purchase, stock, TimeDealPurchaseCancelReason.PAYMENT_FAILED.name());

            assertThatThrownBy(() -> timeDealPolicy.confirmSale(purchase, stock, IN_WINDOW))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_INVALID_STATUS_TRANSITION);

            assertThat(purchase.getCancelReason())
                    .isEqualTo(TimeDealPurchaseCancelReason.PAYMENT_FAILED.name());
            assertThat(stock.getAvailableQuantity()).isEqualTo(INITIAL_QUANTITY);
            assertThat(stock.getSoldQuantity()).isZero();
            assertThat(totalQuantity(stock)).isEqualTo(INITIAL_QUANTITY);
        }

        @Test
        @DisplayName("다른 타임딜의 재고가 넘어오면 예외")
        void 짝_불일치() {
            TimeDeal timeDeal = activeTimeDeal();
            TimeDealStock stock = stockOf(timeDeal, INITIAL_QUANTITY);
            TimeDealPurchase purchase = timeDealPolicy.reserve(
                    timeDeal, stock, UUID.randomUUID(), UUID.randomUUID(), 5, 0, IN_WINDOW);
            TimeDealStock otherStock = stockOf(activeTimeDeal(), INITIAL_QUANTITY);

            assertThatThrownBy(() -> timeDealPolicy.confirmSale(purchase, otherStock, IN_WINDOW))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_STOCK_MISMATCH);
        }
    }


    @Nested
    @DisplayName("선점 만료 정리")
    class ExpireReservation {

        @Test
        @DisplayName("구매 이력이 CANCELLED가 되고 재고가 복구된다")
        void 만료정리_성공() {
            TimeDeal timeDeal = activeTimeDeal();
            TimeDealStock stock = stockOf(timeDeal, INITIAL_QUANTITY);
            TimeDealPurchase purchase = timeDealPolicy.reserve(
                    timeDeal, stock, UUID.randomUUID(), UUID.randomUUID(), 5, 0, IN_WINDOW);

            timeDealPolicy.expireReservation(purchase, stock, AFTER_RESERVATION_TTL);

            assertThat(purchase.getStatus()).isEqualTo(TimeDealPurchaseStatus.CANCELLED);
            assertThat(stock.getAvailableQuantity()).isEqualTo(INITIAL_QUANTITY);
            assertThat(totalQuantity(stock)).isEqualTo(INITIAL_QUANTITY);
        }

        @Test
        @DisplayName("아직 만료되지 않은 선점을 정리하려 하면 예외 — 배치가 대상을 잘못 고른 경우")
        void 미만료_정리() {
            TimeDeal timeDeal = activeTimeDeal();
            TimeDealStock stock = stockOf(timeDeal, INITIAL_QUANTITY);
            TimeDealPurchase purchase = timeDealPolicy.reserve(
                    timeDeal, stock, UUID.randomUUID(), UUID.randomUUID(), 5, 0, IN_WINDOW);

            assertThatThrownBy(() -> timeDealPolicy.expireReservation(purchase, stock, IN_WINDOW))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_INVALID_STATUS_TRANSITION);
        }
    }


    @Nested
    @DisplayName("구매 취소")
    class CancelPurchase {

        @Test
        @DisplayName("RESERVED 취소는 선점 수량을 판매 가능 수량으로 되돌린다")
        void 선점_취소() {
            TimeDeal timeDeal = activeTimeDeal();
            TimeDealStock stock = stockOf(timeDeal, INITIAL_QUANTITY);
            TimeDealPurchase purchase = timeDealPolicy.reserve(
                    timeDeal, stock, UUID.randomUUID(), UUID.randomUUID(), 5, 0, IN_WINDOW);

            timeDealPolicy.cancelPurchase(
                    purchase, stock, TimeDealPurchaseCancelReason.PAYMENT_FAILED.name());

            assertThat(purchase.getStatus()).isEqualTo(TimeDealPurchaseStatus.CANCELLED);
            assertThat(stock.getAvailableQuantity()).isEqualTo(INITIAL_QUANTITY);
            assertThat(stock.getReservedQuantity()).isZero();
            assertThat(totalQuantity(stock)).isEqualTo(INITIAL_QUANTITY);
        }

        @Test
        @DisplayName("CONFIRMED 취소는 판매 수량을 판매 가능 수량으로 바로 되돌린다")
        void 판매확정_취소() {
            TimeDeal timeDeal = activeTimeDeal();
            TimeDealStock stock = stockOf(timeDeal, INITIAL_QUANTITY);
            TimeDealPurchase purchase = timeDealPolicy.reserve(
                    timeDeal, stock, UUID.randomUUID(), UUID.randomUUID(), 5, 0, IN_WINDOW);
            timeDealPolicy.confirmSale(purchase, stock, IN_WINDOW);

            timeDealPolicy.cancelPurchase(
                    purchase, stock, TimeDealPurchaseCancelReason.ORDER_CANCELED.name());

            assertThat(stock.getSoldQuantity()).isZero();
            assertThat(stock.getReservedQuantity()).isZero();
            assertThat(stock.getAvailableQuantity()).isEqualTo(INITIAL_QUANTITY);
            assertThat(totalQuantity(stock)).isEqualTo(INITIAL_QUANTITY);
        }

        @Test
        @DisplayName("이미 취소된 건을 다시 취소해도 예외 없이 재고가 이중 복구되지 않는다")
        void 취소_멱등() {
            TimeDeal timeDeal = activeTimeDeal();
            TimeDealStock stock = stockOf(timeDeal, INITIAL_QUANTITY);
            TimeDealPurchase purchase = timeDealPolicy.reserve(
                    timeDeal, stock, UUID.randomUUID(), UUID.randomUUID(), 5, 0, IN_WINDOW);
            timeDealPolicy.cancelPurchase(
                    purchase, stock, TimeDealPurchaseCancelReason.ORDER_CANCELED.name());

            assertThatCode(() -> timeDealPolicy.cancelPurchase(
                    purchase, stock, TimeDealPurchaseCancelReason.ORDER_CANCELED.name()))
                    .doesNotThrowAnyException();

            assertThat(stock.getAvailableQuantity()).isEqualTo(INITIAL_QUANTITY);
            assertThat(totalQuantity(stock)).isEqualTo(INITIAL_QUANTITY);
        }

        @Test
        @DisplayName("사유 없이도 취소할 수 있다")
        void 사유_null() {
            TimeDeal timeDeal = activeTimeDeal();
            TimeDealStock stock = stockOf(timeDeal, INITIAL_QUANTITY);
            TimeDealPurchase purchase = timeDealPolicy.reserve(
                    timeDeal, stock, UUID.randomUUID(), UUID.randomUUID(), 5, 0, IN_WINDOW);

            assertThatCode(() -> timeDealPolicy.cancelPurchase(purchase, stock, null))
                    .doesNotThrowAnyException();
            assertThat(purchase.getCancelReason()).isNull();
        }

        @Test
        @DisplayName("재고가 복구돼도 ENDED인 타임딜의 상태는 되돌리지 않는다")
        void 종료상태_유지() {
            TimeDeal timeDeal = activeTimeDeal();
            TimeDealStock stock = stockOf(timeDeal, 5);
            TimeDealPurchase purchase = timeDealPolicy.reserve(
                    timeDeal, stock, UUID.randomUUID(), UUID.randomUUID(), 5, 0, IN_WINDOW);

            timeDealPolicy.cancelPurchase(
                    purchase, stock, TimeDealPurchaseCancelReason.ORDER_CANCELED.name());

            assertThat(stock.getAvailableQuantity()).isEqualTo(5);
            assertThat(timeDeal.getStatus()).isEqualTo(TimeDealStatus.ENDED);
        }
    }


    @Nested
    @DisplayName("재고 수동 조정")
    class AdjustStock {

        @Test
        @DisplayName("양수는 물량을 추가하고 음수는 회수한다 — 총 재고 자체가 바뀐다")
        void 조정_성공() {
            TimeDeal timeDeal = activeTimeDeal();
            TimeDealStock stock = stockOf(timeDeal, INITIAL_QUANTITY);
            int before = totalQuantity(stock);

            timeDealPolicy.adjustStock(timeDeal, stock, 20);
            assertThat(totalQuantity(stock)).isEqualTo(before + 20);

            timeDealPolicy.adjustStock(timeDeal, stock, -30);
            assertThat(totalQuantity(stock)).isEqualTo(before - 10);
        }

        @Test
        @DisplayName("ENDED 타임딜의 재고는 조정할 수 없다")
        void 종료딜_조정불가() {
            TimeDeal timeDeal = activeTimeDeal();
            TimeDealStock stock = stockOf(timeDeal, INITIAL_QUANTITY);
            timeDeal.end();

            assertThatThrownBy(() -> timeDealPolicy.adjustStock(timeDeal, stock, 10))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_STOCK_ADJUST_NOT_ALLOWED);
        }

        @Test
        @DisplayName("STOPPED 타임딜의 재고는 조정할 수 없다")
        void 중단딜_조정불가() {
            TimeDeal timeDeal = activeTimeDeal();
            TimeDealStock stock = stockOf(timeDeal, INITIAL_QUANTITY);
            timeDeal.stop();

            assertThatThrownBy(() -> timeDealPolicy.adjustStock(timeDeal, stock, 10))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_STOCK_ADJUST_NOT_ALLOWED);
        }

        @Test
        @DisplayName("선점된 수량을 침범하는 회수는 막는다")
        void 선점침범_조정불가() {
            TimeDeal timeDeal = activeTimeDeal();
            TimeDealStock stock = stockOf(timeDeal, 10);
            timeDealPolicy.reserve(
                    timeDeal, stock, UUID.randomUUID(), UUID.randomUUID(), 5, 0, IN_WINDOW);

            assertThatThrownBy(() -> timeDealPolicy.adjustStock(timeDeal, stock, -6))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_STOCK_INSUFFICIENT);
        }
    }


    @Nested
    @DisplayName("재고 할당")
    class AllocateStock {

        @Test
        @DisplayName("할당된 재고는 타임딜 ID를 참조하고 전량이 판매 가능 수량이다")
        void 할당_성공() {
            TimeDeal timeDeal = scheduledTimeDeal(MAX_PURCHASE_QUANTITY);

            TimeDealStock stock = timeDealPolicy.allocateStock(timeDeal, INITIAL_QUANTITY, 10);

            assertThat(stock.getTimeDealId()).isEqualTo(timeDeal.getId());
            assertThat(stock.getAvailableQuantity()).isEqualTo(INITIAL_QUANTITY);
        }

        @Test
        @DisplayName("1인당 최대 구매 수량이 초기 재고보다 크면 예외")
        void 한도가_재고초과() {
            TimeDeal timeDeal = scheduledTimeDeal(10);

            assertThatThrownBy(() -> timeDealPolicy.allocateStock(timeDeal, 5, 1))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_MAX_PURCHASE_QUANTITY_EXCEEDS_STOCK);
        }

        @Test
        @DisplayName("저장 전 타임딜을 넘기면 예외 — ID가 null이라 걸러진다")
        void 저장전_타임딜() {
            TimeDeal unsavedTimeDeal = TimeDeal.create(
                    UUID.randomUUID(), UUID.randomUUID(), null,
                    "산지직송 사과 5kg", null, TimeDealProductGrade.NORMAL, "경북 안동", HARVESTED_AT,
                    10_000L, BigDecimal.valueOf(30),
                    START_AT, END_AT, MAX_PURCHASE_QUANTITY, CREATED_AT);

            assertThatThrownBy(() -> timeDealPolicy.allocateStock(unsavedTimeDeal, INITIAL_QUANTITY, 10))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }
    }


    @Nested
    @DisplayName("타임딜 삭제")
    class Delete {

        @Test
        @DisplayName("타임딜과 재고가 함께 삭제된다")
        void 삭제_성공() {
            TimeDeal timeDeal = scheduledTimeDeal(MAX_PURCHASE_QUANTITY);
            TimeDealStock stock = stockOf(timeDeal, INITIAL_QUANTITY);

            timeDealPolicy.delete(timeDeal, stock, "tester");

            assertThat(timeDeal.isDeleted()).isTrue();
            assertThat(stock.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("선점이 남은 SCHEDULED 타임딜은 삭제할 수 없고, 타임딜도 삭제 표시되지 않는다")
        void 선점남은_예정딜_삭제불가() {
            // NOTE: 지연 평가 설계상 활성화 배치 전(SCHEDULED)에도 구매가 들어올 수 있어,
            // SCHEDULED가 reserved == 0을 함의하지 않는다.
            TimeDeal timeDeal = scheduledTimeDeal(MAX_PURCHASE_QUANTITY);
            TimeDealStock stock = stockOf(timeDeal, INITIAL_QUANTITY);
            timeDealPolicy.reserve(
                    timeDeal, stock, UUID.randomUUID(), UUID.randomUUID(), 5, 0, IN_WINDOW);

            assertThatThrownBy(() -> timeDealPolicy.delete(timeDeal, stock, "tester"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_STOCK_DELETE_NOT_ALLOWED);

            assertThat(timeDeal.isDeleted()).isFalse();
            assertThat(stock.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("판매 중인 타임딜은 삭제할 수 없고 재고도 삭제되지 않는다")
        void 판매중_삭제불가() {
            TimeDeal timeDeal = activeTimeDeal();
            TimeDealStock stock = stockOf(timeDeal, INITIAL_QUANTITY);

            assertThatThrownBy(() -> timeDealPolicy.delete(timeDeal, stock, "tester"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_ACTIVE_DELETE_NOT_ALLOWED);

            assertThat(stock.isDeleted()).isFalse();
        }
    }


    @Nested
    @DisplayName("판매 기간 종료")
    class EndBySalePeriodEnd {

        @Test
        @DisplayName("판매 기간이 지났으면 ENDED로 종료된다")
        void 종료_성공() {
            TimeDeal timeDeal = activeTimeDeal();

            timeDealPolicy.endBySalePeriodEnd(timeDeal, AFTER_END);

            assertThat(timeDeal.getStatus()).isEqualTo(TimeDealStatus.ENDED);
        }

        @Test
        @DisplayName("판매 기간이 남아 있으면 종료할 수 없다 — 소진 조기 종료 경로와 구분된다")
        void 기간중_종료불가() {
            TimeDeal timeDeal = activeTimeDeal();

            assertThatThrownBy(() -> timeDealPolicy.endBySalePeriodEnd(timeDeal, IN_WINDOW))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_SALE_PERIOD_NOT_ENDED);
        }

        @Test
        @DisplayName("activate가 한 번도 돌지 않은 SCHEDULED 타임딜도 종료할 수 있다")
        void 예정딜_종료() {
            TimeDeal timeDeal = scheduledTimeDeal(MAX_PURCHASE_QUANTITY);

            timeDealPolicy.endBySalePeriodEnd(timeDeal, AFTER_END);

            assertThat(timeDeal.getStatus()).isEqualTo(TimeDealStatus.ENDED);
        }
    }
}