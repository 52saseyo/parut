package com.parut.product.timedeal.domain.timedealstock;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeDealStockTest {

    private static final int INITIAL_QUANTITY = 100;
    private static final int LOW_STOCK_THRESHOLD = 10;

    private static TimeDealStock stock() {
        return TimeDealStock.create(UUID.randomUUID(), INITIAL_QUANTITY, LOW_STOCK_THRESHOLD);
    }

    private static int totalQuantity(TimeDealStock stock) {
        return stock.getAvailableQuantity() + stock.getReservedQuantity() + stock.getSoldQuantity();
    }

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("생성 직후 선점·판매 수량은 0이고 전량이 판매 가능 수량이다")
        void 생성_성공() {
            TimeDealStock stock = stock();

            assertThat(stock.getAvailableQuantity()).isEqualTo(INITIAL_QUANTITY);
            assertThat(stock.getReservedQuantity()).isZero();
            assertThat(stock.getSoldQuantity()).isZero();
        }

        @Test
        @DisplayName("초기 재고가 1개 미만이면 예외")
        void 초기재고_하한() {
            assertThatThrownBy(() -> TimeDealStock.create(UUID.randomUUID(), 0, 0))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_INVALID_STOCK_QUANTITY);
        }

        @Test
        @DisplayName("임계 수량이 초기 재고보다 크면 예외 — 도달할 수 없는 임계치를 막는다")
        void 임계치_상한() {
            assertThatThrownBy(() -> TimeDealStock.create(UUID.randomUUID(), 10, 11))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_INVALID_LOW_STOCK_THRESHOLD);
        }

        @Test
        @DisplayName("임계 수량이 음수면 예외")
        void 임계치_음수() {
            assertThatThrownBy(() -> TimeDealStock.create(UUID.randomUUID(), 10, -1))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_INVALID_LOW_STOCK_THRESHOLD);
        }

        @Test
        @DisplayName("타임딜 ID가 null이면 예외 — 저장 전 TimeDeal의 ID를 넘긴 경우가 여기서 걸린다")
        void 타임딜ID_null() {
            assertThatThrownBy(() -> TimeDealStock.create(null, 10, 1))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Nested
    @DisplayName("재고 선점")
    class Reserve {

        @Test
        @DisplayName("선점하면 판매 가능 수량이 줄고 선점 수량이 늘어난다")
        void 선점_성공() {
            TimeDealStock stock = stock();

            stock.reserve(30);

            assertThat(stock.getAvailableQuantity()).isEqualTo(70);
            assertThat(stock.getReservedQuantity()).isEqualTo(30);
            assertThat(stock.getSoldQuantity()).isZero();
        }

        @Test
        @DisplayName("판매 가능 수량을 넘겨 선점하면 예외")
        void 재고_부족() {
            TimeDealStock stock = stock();

            assertThatThrownBy(() -> stock.reserve(INITIAL_QUANTITY + 1))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_STOCK_INSUFFICIENT);
        }

        @Test
        @DisplayName("선점 수량이 0 이하면 예외")
        void 선점수량_하한() {
            TimeDealStock stock = stock();

            assertThatThrownBy(() -> stock.reserve(0))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_INVALID_PURCHASE_QUANTITY);
        }

        @Test
        @DisplayName("삭제된 재고에서는 선점할 수 없다 — 삭제된 타임딜이 다시 유통되는 것을 막는다")
        void 삭제된_재고_선점_불가() {
            TimeDealStock stock = stock();
            stock.softDelete("tester");

            assertThatThrownBy(() -> stock.reserve(1))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_STOCK_DELETED);
        }
    }

    @Nested
    @DisplayName("판매 확정과 취소")
    class SaleAndCancel {

        @Test
        @DisplayName("판매 확정은 선점 수량을 판매 수량으로 옮긴다")
        void 판매_확정() {
            TimeDealStock stock = stock();
            stock.reserve(30);

            stock.confirmSale(30);

            assertThat(stock.getAvailableQuantity()).isEqualTo(70);
            assertThat(stock.getReservedQuantity()).isZero();
            assertThat(stock.getSoldQuantity()).isEqualTo(30);
        }

        @Test
        @DisplayName("선점 수량보다 많이 확정하면 예외")
        void 확정_수량_초과() {
            TimeDealStock stock = stock();
            stock.reserve(10);

            assertThatThrownBy(() -> stock.confirmSale(11))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_NEGATIVE_STOCK_QUANTITY);
        }

        @Test
        @DisplayName("선점 취소는 선점 수량을 판매 가능 수량으로 되돌린다")
        void 선점_취소() {
            TimeDealStock stock = stock();
            stock.reserve(30);

            stock.cancelReservation(30);

            assertThat(stock.getAvailableQuantity()).isEqualTo(INITIAL_QUANTITY);
            assertThat(stock.getReservedQuantity()).isZero();
        }

        @Test
        @DisplayName("선점 수량보다 많이 취소하면 예외")
        void 선점_취소_초과() {
            TimeDealStock stock = stock();
            stock.reserve(10);

            assertThatThrownBy(() -> stock.cancelReservation(11))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_NEGATIVE_STOCK_QUANTITY);
        }

        @Test
        @DisplayName("판매 확정 취소는 선점을 거치지 않고 판매 가능 수량으로 바로 복구한다")
        void 판매_확정_취소() {
            TimeDealStock stock = stock();
            stock.reserve(30);
            stock.confirmSale(30);

            stock.cancelSale(10);

            assertThat(stock.getSoldQuantity()).isEqualTo(20);
            assertThat(stock.getAvailableQuantity()).isEqualTo(80);
            assertThat(stock.getReservedQuantity()).isZero();
        }

        @Test
        @DisplayName("판매 수량보다 많이 취소하면 예외")
        void 판매_취소_초과() {
            TimeDealStock stock = stock();
            stock.reserve(10);
            stock.confirmSale(10);

            assertThatThrownBy(() -> stock.cancelSale(11))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_NEGATIVE_STOCK_QUANTITY);
        }

        @Test
        @DisplayName("구매 흐름 전체에서 세 수량의 합계는 변하지 않는다 — 총합 불변식")
        void 총합_불변식() {
            TimeDealStock stock = stock();
            assertThat(totalQuantity(stock)).isEqualTo(INITIAL_QUANTITY);

            stock.reserve(40);
            assertThat(totalQuantity(stock)).isEqualTo(INITIAL_QUANTITY);

            stock.confirmSale(25);
            assertThat(totalQuantity(stock)).isEqualTo(INITIAL_QUANTITY);

            stock.cancelReservation(15);
            assertThat(totalQuantity(stock)).isEqualTo(INITIAL_QUANTITY);

            stock.cancelSale(25);
            assertThat(totalQuantity(stock)).isEqualTo(INITIAL_QUANTITY);
            assertThat(stock.getAvailableQuantity()).isEqualTo(INITIAL_QUANTITY);
        }
    }

    @Nested
    @DisplayName("수동 재고 조정")
    class AdjustAvailableQuantity {

        @Test
        @DisplayName("양수 delta는 판매 가능 수량을 늘리고 선점·판매 수량은 건드리지 않는다")
        void 물량_추가() {
            TimeDealStock stock = stock();
            stock.reserve(30);

            stock.adjustAvailableQuantity(50);

            assertThat(stock.getAvailableQuantity()).isEqualTo(120);
            assertThat(stock.getReservedQuantity()).isEqualTo(30);
            assertThat(stock.getSoldQuantity()).isZero();
        }

        @Test
        @DisplayName("음수 delta는 판매 가능 수량을 줄인다")
        void 물량_회수() {
            TimeDealStock stock = stock();

            stock.adjustAvailableQuantity(-40);

            assertThat(stock.getAvailableQuantity()).isEqualTo(60);
        }

        @Test
        @DisplayName("선점된 수량은 회수로 침범할 수 없다 — 잔여 판매 가능 수량까지만 줄어든다")
        void 선점분_침범_불가() {
            TimeDealStock stock = stock();
            stock.reserve(70);

            assertThatThrownBy(() -> stock.adjustAvailableQuantity(-31))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_STOCK_INSUFFICIENT);
        }

        @Test
        @DisplayName("잔여 판매 가능 수량 전량 회수는 허용된다")
        void 전량_회수_허용() {
            TimeDealStock stock = stock();
            stock.reserve(70);

            stock.adjustAvailableQuantity(-30);

            assertThat(stock.getAvailableQuantity()).isZero();
            assertThat(stock.getReservedQuantity()).isEqualTo(70);
        }

        @Test
        @DisplayName("delta가 0이면 예외 — 의미 없는 조정 요청")
        void delta_0() {
            TimeDealStock stock = stock();

            assertThatThrownBy(() -> stock.adjustAvailableQuantity(0))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_INVALID_STOCK_ADJUST_QUANTITY);
        }

        @Test
        @DisplayName("delta가 null이면 예외")
        void delta_null() {
            TimeDealStock stock = stock();

            assertThatThrownBy(() -> stock.adjustAvailableQuantity(null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Nested
    @DisplayName("소진 판정")
    class IsDepleted {

        @Test
        @DisplayName("판매 가능 수량이 남아 있으면 소진이 아니다")
        void 미소진() {
            assertThat(stock().isDepleted()).isFalse();
        }

        @Test
        @DisplayName("전량이 선점되면 결제 확정 전에도 소진으로 본다")
        void 전량_선점시_소진() {
            TimeDealStock stock = stock();

            stock.reserve(INITIAL_QUANTITY);

            assertThat(stock.isDepleted()).isTrue();
        }

        @Test
        @DisplayName("선점이 취소되면 다시 소진이 아니게 된다 — 다만 TimeDeal 상태는 복구하지 않는다")
        void 선점_취소시_미소진() {
            TimeDealStock stock = stock();
            stock.reserve(INITIAL_QUANTITY);

            stock.cancelReservation(INITIAL_QUANTITY);

            assertThat(stock.isDepleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("삭제")
    class SoftDelete {

        @Test
        @DisplayName("선점·판매 수량이 없으면 삭제할 수 있다")
        void 삭제_성공() {
            TimeDealStock stock = stock();

            stock.softDelete("tester");

            assertThat(stock.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("선점된 수량이 있으면 삭제할 수 없다")
        void 선점중_삭제_불가() {
            TimeDealStock stock = stock();
            stock.reserve(1);

            assertThatThrownBy(() -> stock.softDelete("tester"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_STOCK_DELETE_NOT_ALLOWED);
        }

        @Test
        @DisplayName("판매된 수량이 있으면 삭제할 수 없다")
        void 판매이력_삭제_불가() {
            TimeDealStock stock = stock();
            stock.reserve(1);
            stock.confirmSale(1);

            assertThatThrownBy(() -> stock.softDelete("tester"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_STOCK_DELETE_NOT_ALLOWED);
        }
    }
}