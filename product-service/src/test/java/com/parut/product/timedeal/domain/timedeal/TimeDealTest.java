package com.parut.product.timedeal.domain.timedeal;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeDealTest {

    // 시각을 파라미터로 주입받는 구조라 절대 시각을 고정해 경계까지 재현할 수 있다.
    private static final Instant CREATED_AT = Instant.parse("2026-09-04T10:00:00Z");
    private static final Instant START_AT = Instant.parse("2026-09-04T11:00:00Z");
    private static final Instant END_AT = Instant.parse("2026-09-04T12:00:00Z");
    private static final Instant IN_WINDOW = Instant.parse("2026-09-04T11:30:00Z");
    private static final Instant BEFORE_START = Instant.parse("2026-09-04T10:59:59Z");
    private static final Instant AFTER_END = Instant.parse("2026-09-04T12:00:01Z");

    private static TimeDeal scheduledDeal() {
        return TimeDeal.create(
                UUID.randomUUID(), 10_000L, BigDecimal.valueOf(30), START_AT, END_AT, 5, CREATED_AT);
    }

    private static TimeDeal activeDeal() {
        TimeDeal deal = scheduledDeal();
        deal.activate(IN_WINDOW);
        return deal;
    }

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("생성 직후 상태는 SCHEDULED이고 dealPrice는 할인율로 계산된다")
        void 생성_성공() {
            TimeDeal deal = scheduledDeal();

            assertThat(deal.getStatus()).isEqualTo(TimeDealStatus.SCHEDULED);
            assertThat(deal.getDealPrice()).isEqualTo(7_000L);
            assertThat(deal.getOriginalPrice()).isEqualTo(10_000L);
        }

        @Test
        @DisplayName("dealPrice는 원 단위 아래를 절삭한다")
        void dealPrice_절삭() {
            TimeDeal deal = TimeDeal.create(
                    UUID.randomUUID(), 10_000L, BigDecimal.valueOf(33.33), START_AT, END_AT, 5, CREATED_AT);

            assertThat(deal.getDealPrice()).isEqualTo(6_667L);
        }

        @Test
        @DisplayName("할인율 0%는 정가 판매로 허용된다")
        void 할인율_0_허용() {
            TimeDeal deal = TimeDeal.create(
                    UUID.randomUUID(), 10_000L, BigDecimal.ZERO, START_AT, END_AT, 5, CREATED_AT);

            assertThat(deal.getDealPrice()).isEqualTo(deal.getOriginalPrice());
        }

        @Test
        @DisplayName("종료 시각이 시작 시각보다 앞이면 예외")
        void 기간_역전() {
            assertThatThrownBy(() -> TimeDeal.create(
                    UUID.randomUUID(), 10_000L, BigDecimal.valueOf(30), END_AT, START_AT, 5, CREATED_AT))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_INVALID_PERIOD);
        }

        @Test
        @DisplayName("종료 시각이 기준 시각보다 과거면 예외")
        void 이미_끝난_기간() {
            assertThatThrownBy(() -> TimeDeal.create(
                    UUID.randomUUID(), 10_000L, BigDecimal.valueOf(30), START_AT, END_AT, 5, AFTER_END))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_INVALID_PERIOD);
        }

        @Test
        @DisplayName("필수값이 null이면 예외")
        void 필수값_null() {
            assertThatThrownBy(() -> TimeDeal.create(
                    UUID.randomUUID(), null, BigDecimal.valueOf(30), START_AT, END_AT, 5, CREATED_AT))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }

        @Test
        @DisplayName("정가가 0 이하면 예외")
        void 잘못된_가격() {
            assertThatThrownBy(() -> TimeDeal.create(
                    UUID.randomUUID(), 0L, BigDecimal.valueOf(30), START_AT, END_AT, 5, CREATED_AT))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_INVALID_PRICE);
        }

        @Test
        @DisplayName("할인율이 100 이상이면 예외")
        void 할인율_상한() {
            assertThatThrownBy(() -> TimeDeal.create(
                    UUID.randomUUID(), 10_000L, BigDecimal.valueOf(100), START_AT, END_AT, 5, CREATED_AT))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_INVALID_DISCOUNT_RATE);
        }

        @Test
        @DisplayName("최대 구매 수량이 1개 미만이면 예외")
        void 최대구매수량_하한() {
            assertThatThrownBy(() -> TimeDeal.create(
                    UUID.randomUUID(), 10_000L, BigDecimal.valueOf(30), START_AT, END_AT, 0, CREATED_AT))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_INVALID_MAX_PURCHASE_QUANTITY);
        }
    }

    @Nested
    @DisplayName("수정 (PATCH 부분 수정)")
    class Update {

        @Test
        @DisplayName("모든 파라미터가 null이면 아무 값도 바뀌지 않는다")
        void 전체_null이면_유지() {
            TimeDeal deal = scheduledDeal();

            deal.update(null, null, null, null, null, CREATED_AT);

            assertThat(deal.getOriginalPrice()).isEqualTo(10_000L);
            assertThat(deal.getDealPrice()).isEqualTo(7_000L);
            assertThat(deal.getStartAt()).isEqualTo(START_AT);
            assertThat(deal.getEndAt()).isEqualTo(END_AT);
            assertThat(deal.getMaxPurchaseQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("넘어온 값만 바뀌고 나머지는 유지된다")
        void 부분_수정() {
            TimeDeal deal = scheduledDeal();

            deal.update(null, null, null, null, 9, CREATED_AT);

            assertThat(deal.getMaxPurchaseQuantity()).isEqualTo(9);
            assertThat(deal.getOriginalPrice()).isEqualTo(10_000L);
        }

        @Test
        @DisplayName("정가만 바꿔도 dealPrice가 다시 계산된다")
        void 정가만_바꿔도_할인가_재계산() {
            TimeDeal deal = scheduledDeal();

            deal.update(20_000L, null, null, null, null, CREATED_AT);

            assertThat(deal.getDealPrice()).isEqualTo(14_000L);
        }

        @Test
        @DisplayName("시작 시각만 넘겨 기존 종료 시각과 역전되면 예외 — 병합 후 값으로 검증한다")
        void 병합_후_검증() {
            TimeDeal deal = scheduledDeal();
            Instant afterExistingEnd = Instant.parse("2026-09-04T13:00:00Z");

            assertThatThrownBy(() -> deal.update(null, null, afterExistingEnd, null, null, CREATED_AT))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_INVALID_PERIOD);
        }

        @Test
        @DisplayName("SCHEDULED가 아니면 수정할 수 없다")
        void 활성화된_딜은_수정_불가() {
            TimeDeal deal = activeDeal();

            assertThatThrownBy(() -> deal.update(20_000L, null, null, null, null, IN_WINDOW))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_UPDATE_NOT_ALLOWED);
        }

        @Test
        @DisplayName("삭제된 타임딜은 수정할 수 없다")
        void 삭제된_딜은_수정_불가() {
            TimeDeal deal = scheduledDeal();
            deal.softDelete("tester");

            assertThatThrownBy(() -> deal.update(20_000L, null, null, null, null, CREATED_AT))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_DELETED);
        }
    }

    @Nested
    @DisplayName("상태 전이")
    class StatusTransition {

        @Test
        @DisplayName("판매 기간 안에서 활성화할 수 있다")
        void 활성화_성공() {
            TimeDeal deal = scheduledDeal();

            deal.activate(IN_WINDOW);

            assertThat(deal.getStatus()).isEqualTo(TimeDealStatus.ACTIVE);
        }

        @Test
        @DisplayName("시작 전에는 활성화할 수 없다")
        void 시작_전_활성화_불가() {
            TimeDeal deal = scheduledDeal();

            assertThatThrownBy(() -> deal.activate(BEFORE_START))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_SALE_PERIOD_INVALID);
        }

        @Test
        @DisplayName("종료 시각이 지난 뒤에는 활성화할 수 없다")
        void 종료_후_활성화_불가() {
            TimeDeal deal = scheduledDeal();

            assertThatThrownBy(() -> deal.activate(AFTER_END))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_SALE_PERIOD_INVALID);
        }

        @Test
        @DisplayName("이미 활성화된 타임딜은 다시 활성화할 수 없다")
        void 중복_활성화_불가() {
            TimeDeal deal = activeDeal();

            assertThatThrownBy(() -> deal.activate(IN_WINDOW))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_INVALID_STATUS_TRANSITION);
        }

        @Test
        @DisplayName("ACTIVE인 타임딜을 종료할 수 있다")
        void 활성_딜_종료() {
            TimeDeal deal = activeDeal();

            deal.end();

            assertThat(deal.getStatus()).isEqualTo(TimeDealStatus.ENDED);
        }

        @Test
        @DisplayName("활성화된 적 없는 SCHEDULED 딜도 종료할 수 있다 — 배치가 정리하지 못해 영구 잔존하는 것을 막는다")
        void 예정_딜_종료() {
            TimeDeal deal = scheduledDeal();

            deal.end();

            assertThat(deal.getStatus()).isEqualTo(TimeDealStatus.ENDED);
        }

        @Test
        @DisplayName("이미 종료된 딜은 다시 종료할 수 없다")
        void 중복_종료_불가() {
            TimeDeal deal = activeDeal();
            deal.end();

            assertThatThrownBy(deal::end)
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_INVALID_STATUS_TRANSITION);
        }

        @Test
        @DisplayName("ACTIVE인 딜을 강제 종료할 수 있다")
        void 강제_종료() {
            TimeDeal deal = activeDeal();

            deal.stop();

            assertThat(deal.getStatus()).isEqualTo(TimeDealStatus.STOPPED);
        }

        @Test
        @DisplayName("SCHEDULED인 딜은 강제 종료할 수 없다 — 삭제 API를 써야 한다")
        void 예정_딜_강제종료_불가() {
            TimeDeal deal = scheduledDeal();

            assertThatThrownBy(deal::stop)
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_NOT_ACTIVE);
        }
    }

    @Nested
    @DisplayName("삭제")
    class SoftDelete {

        @Test
        @DisplayName("SCHEDULED인 딜만 삭제할 수 있다")
        void 예정_딜_삭제() {
            TimeDeal deal = scheduledDeal();

            deal.softDelete("tester");

            assertThat(deal.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("판매 중인 딜은 삭제할 수 없다")
        void 활성_딜_삭제_불가() {
            TimeDeal deal = activeDeal();

            assertThatThrownBy(() -> deal.softDelete("tester"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_ACTIVE_DELETE_NOT_ALLOWED);
        }

        @Test
        @DisplayName("종료된 딜은 삭제할 수 없다")
        void 종료_딜_삭제_불가() {
            TimeDeal deal = activeDeal();
            deal.end();

            assertThatThrownBy(() -> deal.softDelete("tester"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_INVALID_STATUS);
        }
    }

    @Nested
    @DisplayName("구매 가능 여부 검증")
    class ValidatePurchasable {

        @Test
        @DisplayName("배치가 활성화를 못 돌려 SCHEDULED여도 판매 기간 안이면 구매할 수 있다 — 지연 평가")
        void 예정_상태여도_기간_안이면_통과() {
            TimeDeal deal = scheduledDeal();

            deal.validatePurchasable(IN_WINDOW);
        }

        @Test
        @DisplayName("종료 시각 정각은 아직 구매할 수 있다")
        void 종료_경계_통과() {
            TimeDeal deal = activeDeal();

            deal.validatePurchasable(END_AT);
        }

        @Test
        @DisplayName("종료 시각을 1초 넘기면 구매할 수 없다")
        void 종료_직후_불가() {
            TimeDeal deal = activeDeal();

            assertThatThrownBy(() -> deal.validatePurchasable(AFTER_END))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_SALE_PERIOD_INVALID);
        }

        @Test
        @DisplayName("시작 전에는 구매할 수 없다")
        void 시작_전_불가() {
            TimeDeal deal = scheduledDeal();

            assertThatThrownBy(() -> deal.validatePurchasable(BEFORE_START))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_SALE_PERIOD_INVALID);
        }

        @Test
        @DisplayName("강제 종료된 딜은 기간 안이어도 구매할 수 없다")
        void 강제종료_딜_불가() {
            TimeDeal deal = activeDeal();
            deal.stop();

            assertThatThrownBy(() -> deal.validatePurchasable(IN_WINDOW))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_NOT_ACTIVE);
        }

        @Test
        @DisplayName("소진 등으로 종료된 딜은 기간 안이어도 구매할 수 없다")
        void 종료_딜_불가() {
            TimeDeal deal = activeDeal();
            deal.end();

            assertThatThrownBy(() -> deal.validatePurchasable(IN_WINDOW))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_NOT_ACTIVE);
        }

        @Test
        @DisplayName("삭제된 딜은 구매할 수 없다")
        void 삭제된_딜_불가() {
            TimeDeal deal = scheduledDeal();
            deal.softDelete("tester");

            assertThatThrownBy(() -> deal.validatePurchasable(IN_WINDOW))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_DELETED);
        }

        @Test
        @DisplayName("기준 시각이 null이면 예외")
        void 기준시각_null() {
            TimeDeal deal = activeDeal();

            assertThatThrownBy(() -> deal.validatePurchasable(null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Nested
    @DisplayName("1인당 누적 구매 수량 검증")
    class ValidatePurchaseQuantity {

        @Test
        @DisplayName("누적과 요청의 합이 상한과 같으면 통과한다")
        void 상한_경계_통과() {
            TimeDeal deal = scheduledDeal();

            deal.validatePurchaseQuantity(2, 3);
        }

        @Test
        @DisplayName("이미 구매한 수량을 더해 상한을 넘으면 예외 — 주문을 나눠도 우회할 수 없다")
        void 누적으로_상한_초과() {
            TimeDeal deal = scheduledDeal();

            assertThatThrownBy(() -> deal.validatePurchaseQuantity(3, 3))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_EXCEEDS_MAX_PURCHASE_QUANTITY);
        }

        @Test
        @DisplayName("단건 요청만으로 상한을 넘으면 예외")
        void 단건으로_상한_초과() {
            TimeDeal deal = scheduledDeal();

            assertThatThrownBy(() -> deal.validatePurchaseQuantity(6, 0))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_EXCEEDS_MAX_PURCHASE_QUANTITY);
        }

        @Test
        @DisplayName("누적 수량이 null이면 예외")
        void 누적수량_null() {
            TimeDeal deal = scheduledDeal();

            assertThatThrownBy(() -> deal.validatePurchaseQuantity(1, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }

        @Test
        @DisplayName("누적 수량이 음수면 예외")
        void 누적수량_음수() {
            TimeDeal deal = scheduledDeal();

            assertThatThrownBy(() -> deal.validatePurchaseQuantity(1, -1))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}