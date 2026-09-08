package com.parut.product.timedeal.application.command.timedealpurchase;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.timedeal.application.dto.timedealpurchase.TimeDealPurchaseCancelCommand;
import com.parut.product.timedeal.application.dto.timedealpurchase.TimeDealPurchaseConfirmCommand;
import com.parut.product.timedeal.application.dto.timedealpurchase.TimeDealPurchaseReserveCommand;
import com.parut.product.timedeal.application.exception.TimeDealReservationExpiredException;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealRepository;
import com.parut.product.timedeal.application.port.out.timedealpurchase.TimeDealPurchaseRepository;
import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockRepository;
import com.parut.product.timedeal.domain.common.TimeDealPolicy;
import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import com.parut.product.timedeal.domain.timedealpurchase.TimeDealPurchase;
import com.parut.product.timedeal.domain.timedealpurchase.TimeDealPurchaseCancelReason;
import com.parut.product.timedeal.domain.timedealpurchase.TimeDealPurchaseStatus;
import com.parut.product.timedeal.domain.timedealstock.TimeDealStock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// NOTE: Repository만 mock하고 TimeDealPolicy는 실물을 쓴다 — Policy를 mock하면 "서비스가 policy를 불렀다"만
// 검증되고 정작 확인해야 하는 애그리거트 상태 결과가 가려진다.
// NOTE: @Transactional / noRollbackFor는 Spring 프록시가 실행하므로 이 테스트에서는 동작하지 않는다.
// "만료 정리가 커밋되는가"는 통합 테스트 영역이다.
@ExtendWith(MockitoExtension.class)
class TimeDealPurchaseCommandServiceTest {

    private static final Instant START_AT = Instant.parse("2026-09-04T11:00:00Z");
    private static final Instant END_AT = Instant.parse("2099-09-04T12:00:00Z");
    private static final int INITIAL_QUANTITY = 100;
    private static final int MAX_PURCHASE_QUANTITY = 10;

    @Mock
    private TimeDealRepository timeDealRepository;

    @Mock
    private TimeDealStockRepository timeDealStockRepository;

    @Mock
    private TimeDealPurchaseRepository timeDealPurchaseRepository;

    private TimeDealPurchaseCommandService timeDealPurchaseCommandService;

    private TimeDeal timeDeal;
    private TimeDealStock timeDealStock;

    @BeforeEach
    void setUp() {
        // NOTE: TimeDealPolicy는 실물을 쓰므로 @InjectMocks 대신 직접 조립한다.
        timeDealPurchaseCommandService = new TimeDealPurchaseCommandService(
                timeDealRepository,
                timeDealStockRepository,
                timeDealPurchaseRepository,
                new TimeDealPolicy()
        );

        // NOTE: endAt을 먼 미래로 두어 Instant.now()를 쓰는 서비스에서도 판매 기간 안에 들도록 한다.
        timeDeal = TimeDeal.create(
                UUID.randomUUID(), 10_000L, BigDecimal.valueOf(30),
                START_AT, END_AT, MAX_PURCHASE_QUANTITY, START_AT);
        // NOTE: id는 @GeneratedValue라 저장 없이는 null이고, 그러면 짝 검증이 통과할 수 없다.
        ReflectionTestUtils.setField(timeDeal, "id", UUID.randomUUID());
        timeDeal.activate(START_AT);

        timeDealStock = TimeDealStock.create(timeDeal.getId(), INITIAL_QUANTITY, 10);
    }

    private TimeDealPurchaseReserveCommand reserveCommand(int quantity) {
        return new TimeDealPurchaseReserveCommand(
                timeDeal.getId(), UUID.randomUUID(), UUID.randomUUID(), quantity);
    }

    private void givenTimeDealAndStockFound() {
        when(timeDealRepository.findById(timeDeal.getId())).thenReturn(Optional.of(timeDeal));
        when(timeDealStockRepository.findByTimeDealId(timeDeal.getId()))
                .thenReturn(Optional.of(timeDealStock));
    }


    @Nested
    @DisplayName("구매 예약")
    class Reserve {

        @Test
        @DisplayName("구매 이력을 저장하고 재고를 선점한다")
        void 예약_성공() {
            givenTimeDealAndStockFound();
            when(timeDealPurchaseRepository.existsByOrderId(any())).thenReturn(false);
            when(timeDealPurchaseRepository.sumActiveQuantity(any(), any())).thenReturn(0);

            timeDealPurchaseCommandService.reserve(reserveCommand(5));

            verify(timeDealPurchaseRepository).save(any(TimeDealPurchase.class));
            assertThat(timeDealStock.getReservedQuantity()).isEqualTo(5);
            assertThat(timeDealStock.getAvailableQuantity()).isEqualTo(95);
        }

        @Test
        @DisplayName("같은 주문으로 이미 선점했으면 예외이고 조회조차 하지 않는다")
        void orderId_중복() {
            when(timeDealPurchaseRepository.existsByOrderId(any())).thenReturn(true);

            assertThatThrownBy(() -> timeDealPurchaseCommandService.reserve(reserveCommand(5)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_PURCHASE_ALREADY_EXISTS);

            verify(timeDealRepository, never()).findById(any());
            verify(timeDealPurchaseRepository, never()).save(any());
        }

        @Test
        @DisplayName("타임딜이 없으면 404")
        void 타임딜_없음() {
            when(timeDealPurchaseRepository.existsByOrderId(any())).thenReturn(false);
            when(timeDealRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> timeDealPurchaseCommandService.reserve(reserveCommand(5)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_NOT_FOUND);
        }

        @Test
        @DisplayName("재고 정보가 없으면 404")
        void 재고_없음() {
            when(timeDealPurchaseRepository.existsByOrderId(any())).thenReturn(false);
            when(timeDealRepository.findById(any())).thenReturn(Optional.of(timeDeal));
            when(timeDealStockRepository.findByTimeDealId(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> timeDealPurchaseCommandService.reserve(reserveCommand(5)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_STOCK_NOT_FOUND);
        }

        @Test
        @DisplayName("집계된 누적 수량이 실제로 1인당 제한에 반영된다")
        void 누적수량_전달() {
            givenTimeDealAndStockFound();
            when(timeDealPurchaseRepository.existsByOrderId(any())).thenReturn(false);
            // 이미 6개를 확보한 사용자가 5개를 더 요청하면 한도 10개를 넘는다.
            when(timeDealPurchaseRepository.sumActiveQuantity(any(), any())).thenReturn(6);

            assertThatThrownBy(() -> timeDealPurchaseCommandService.reserve(reserveCommand(5)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_EXCEEDS_MAX_PURCHASE_QUANTITY);

            verify(timeDealPurchaseRepository, never()).save(any());
            assertThat(timeDealStock.getReservedQuantity()).isZero();
        }
    }


    @Nested
    @DisplayName("판매 확정")
    class Confirm {

        private TimeDealPurchase reservedPurchase(Instant reservedAt) {
            return TimeDealPurchase.create(
                    timeDeal, UUID.randomUUID(), UUID.randomUUID(), 5, reservedAt);
        }

        @Test
        @DisplayName("확정되면 선점 수량이 판매 수량으로 이동한다")
        void 확정_성공() {
            TimeDealPurchase purchase = reservedPurchase(Instant.now());
            timeDealStock.reserve(5);
            when(timeDealPurchaseRepository.findByOrderId(any())).thenReturn(Optional.of(purchase));
            when(timeDealStockRepository.findByTimeDealId(any())).thenReturn(Optional.of(timeDealStock));

            timeDealPurchaseCommandService.confirm(new TimeDealPurchaseConfirmCommand(UUID.randomUUID()));

            assertThat(purchase.getStatus()).isEqualTo(TimeDealPurchaseStatus.CONFIRMED);
            assertThat(timeDealStock.getSoldQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("선점이 만료됐으면 전용 예외를 던지고, 그 전에 정리는 이미 끝나 있다")
        void 만료_확정실패() {
            // NOTE: 선점 TTL 10분을 넘긴 시점을 만들기 위해 reservedAt을 과거로 준다.
            TimeDealPurchase purchase = reservedPurchase(Instant.now().minusSeconds(3600));
            timeDealStock.reserve(5);
            when(timeDealPurchaseRepository.findByOrderId(any())).thenReturn(Optional.of(purchase));
            when(timeDealStockRepository.findByTimeDealId(any())).thenReturn(Optional.of(timeDealStock));

            assertThatThrownBy(() -> timeDealPurchaseCommandService.confirm(
                    new TimeDealPurchaseConfirmCommand(UUID.randomUUID())))
                    .isInstanceOf(TimeDealReservationExpiredException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_RESERVATION_EXPIRED);

            // 예외를 던지기 전에 정리가 끝나 있어야 한다 — 커밋 여부는 통합 테스트가 확인한다.
            assertThat(purchase.getStatus()).isEqualTo(TimeDealPurchaseStatus.CANCELLED);
            assertThat(timeDealStock.getAvailableQuantity()).isEqualTo(INITIAL_QUANTITY);
            assertThat(timeDealStock.getReservedQuantity()).isZero();
        }

        @Test
        @DisplayName("구매 이력이 없으면 404")
        void 구매이력_없음() {
            when(timeDealPurchaseRepository.findByOrderId(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> timeDealPurchaseCommandService.confirm(
                    new TimeDealPurchaseConfirmCommand(UUID.randomUUID())))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_PURCHASE_NOT_FOUND);
        }
    }


    @Nested
    @DisplayName("구매 취소")
    class Cancel {

        private TimeDealPurchase reservedPurchase() {
            return TimeDealPurchase.create(
                    timeDeal, UUID.randomUUID(), UUID.randomUUID(), 5, Instant.now());
        }

        @Test
        @DisplayName("취소 사유 문구가 그대로 기록되고 재고가 복구된다")
        void 취소_성공() {
            TimeDealPurchase purchase = reservedPurchase();
            timeDealStock.reserve(5);
            when(timeDealPurchaseRepository.findByOrderId(any())).thenReturn(Optional.of(purchase));
            when(timeDealStockRepository.findByTimeDealId(any())).thenReturn(Optional.of(timeDealStock));

            timeDealPurchaseCommandService.cancel(new TimeDealPurchaseCancelCommand(
                    UUID.randomUUID(), TimeDealPurchaseCancelReason.ORDER_CANCELED.name()));

            assertThat(purchase.getStatus()).isEqualTo(TimeDealPurchaseStatus.CANCELLED);
            assertThat(purchase.getCancelReason())
                    .isEqualTo(TimeDealPurchaseCancelReason.ORDER_CANCELED.name());
            assertThat(timeDealStock.getAvailableQuantity()).isEqualTo(INITIAL_QUANTITY);
        }

        @Test
        @DisplayName("카탈로그에 없는 자유 문구도 사유로 쓸 수 있다")
        void 자유문구_사유() {
            TimeDealPurchase purchase = reservedPurchase();
            timeDealStock.reserve(5);
            when(timeDealPurchaseRepository.findByOrderId(any())).thenReturn(Optional.of(purchase));
            when(timeDealStockRepository.findByTimeDealId(any())).thenReturn(Optional.of(timeDealStock));

            timeDealPurchaseCommandService.cancel(
                    new TimeDealPurchaseCancelCommand(UUID.randomUUID(), "판매자 요청으로 취소"));

            assertThat(purchase.getCancelReason()).isEqualTo("판매자 요청으로 취소");
        }

        @Test
        @DisplayName("이미 취소된 건에 다시 취소가 와도 예외 없이 재고가 이중 복구되지 않는다")
        void 취소_멱등() {
            TimeDealPurchase purchase = reservedPurchase();
            timeDealStock.reserve(5);
            when(timeDealPurchaseRepository.findByOrderId(any())).thenReturn(Optional.of(purchase));
            when(timeDealStockRepository.findByTimeDealId(any())).thenReturn(Optional.of(timeDealStock));
            TimeDealPurchaseCancelCommand command = new TimeDealPurchaseCancelCommand(
                    UUID.randomUUID(), TimeDealPurchaseCancelReason.ORDER_CANCELED.name());

            timeDealPurchaseCommandService.cancel(command);
            timeDealPurchaseCommandService.cancel(command);

            assertThat(timeDealStock.getAvailableQuantity()).isEqualTo(INITIAL_QUANTITY);
            assertThat(timeDealStock.getSoldQuantity()).isZero();
        }

        @Test
        @DisplayName("구매 이력이 없으면 404")
        void 구매이력_없음() {
            when(timeDealPurchaseRepository.findByOrderId(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> timeDealPurchaseCommandService.cancel(
                    new TimeDealPurchaseCancelCommand(UUID.randomUUID(), null)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_PURCHASE_NOT_FOUND);
        }
    }
}