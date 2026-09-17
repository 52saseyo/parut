package com.parut.order.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.dto.CancelOrderCommand;
import com.parut.order.order.application.dto.OrderCancelContext;
import com.parut.order.order.application.dto.OrderCancelResult;
import com.parut.order.order.application.port.out.ProductClient;
import com.parut.order.order.application.port.out.TimeDealClient;
import com.parut.order.order.domain.CancelReasonCode;
import com.parut.order.order.domain.CanceledByType;
import com.parut.order.payment.application.port.in.PaymentCancelUseCase;
import com.parut.order.payment.application.port.in.dto.PaymentCancelReceipt;

@ExtendWith(MockitoExtension.class)
class OrderCancelFacadeTest {

    private static final UUID ORDER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b1");
    private static final UUID ITEM_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b2");
    private static final UUID PRODUCT_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b3");
    private static final UUID TIME_DEAL_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b4");
    private static final UUID CUSTOMER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b5");
    private static final String IDEMPOTENCY_KEY = "idem-key-0001";
    private static final long CANCEL_TOTAL_AMOUNT = 18_000L;

    @Mock
    private OrderCancelService orderCancelService;

    @Mock
    private PaymentCancelUseCase paymentCancelUseCase;

    @Mock
    private ProductClient productClient;

    @Mock
    private TimeDealClient timeDealClient;

    @InjectMocks
    private OrderCancelFacade orderCancelFacade;

    @Test
    @DisplayName("일반 상품 취소는 ProductClient로 재고를 복원한다")
    void 일반상품_재고복원() {
        stubCancelFlow(context(PRODUCT_ID, null));

        orderCancelFacade.cancel(command());

        verify(productClient).restoreStock(PRODUCT_ID, ORDER_ID, ITEM_ID);
        verifyNoInteractions(timeDealClient);
    }

    @Test
    @DisplayName("타임딜 주문 취소는 TimeDealClient로 재고를 복원하며 orderItemId를 넘기지 않는다")
    void 타임딜_재고복원() {
        stubCancelFlow(context(null, TIME_DEAL_ID));

        orderCancelFacade.cancel(command());

        verify(timeDealClient).restoreStock(ORDER_ID, CancelReasonCode.CUSTOMER_CANCEL.name());
        verifyNoInteractions(productClient);
    }

    @Test
    @DisplayName("타임딜 아이템이 여러 개여도 재고 해제는 주문 단위 API라 한 번만 호출한다")
    void 타임딜_재고복원_단일호출() {
        stubCancelFlow(new OrderCancelContext(
                ORDER_ID, 30_000L, 0L, 30_000L,
                List.of(new OrderCancelContext.CancelTargetItem(ITEM_ID, null, TIME_DEAL_ID),
                        new OrderCancelContext.CancelTargetItem(UUID.randomUUID(), null, TIME_DEAL_ID))));

        orderCancelFacade.cancel(command());

        verify(timeDealClient, times(1)).restoreStock(ORDER_ID, CancelReasonCode.CUSTOMER_CANCEL.name());
    }

    @Test
    @DisplayName("재고 복원이 실패해도 취소는 롤백하지 않고 결과를 반환한다")
    void 재고복원_실패해도_취소유지() {
        stubCancelFlow(context(PRODUCT_ID, null));
        doThrow(new IllegalStateException("Product 장애"))
                .when(productClient).restoreStock(any(), any(), any());

        OrderCancelResult result = orderCancelFacade.cancel(command());

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("PG 취소가 실패하면 DB를 갱신하지 않고 예외를 전파한다")
    void PG취소_실패() {
        when(orderCancelService.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(orderCancelService.loadForCancel(any())).thenReturn(context(PRODUCT_ID, null));
        when(paymentCancelUseCase.cancelOnPg(eq(ORDER_ID), anyLong(), anyString()))
                .thenThrow(new BusinessException(ErrorCode.PG_CANCEL_FAILED));

        assertThatThrownBy(() -> orderCancelFacade.cancel(command()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PG_CANCEL_FAILED);

        verify(orderCancelService, never()).applyCancel(any(), any(), any());
        verifyNoInteractions(productClient, timeDealClient);
    }

    @Test
    @DisplayName("같은 멱등키로 재요청하면 기존 취소 결과를 그대로 반환한다")
    void 멱등_재요청() {
        OrderCancelResult existing = result();
        when(orderCancelService.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.of(existing));

        assertThat(orderCancelFacade.cancel(command())).isSameAs(existing);

        verify(orderCancelService, never()).loadForCancel(any());
        verifyNoInteractions(paymentCancelUseCase, productClient, timeDealClient);
    }

    private void stubCancelFlow(OrderCancelContext context) {
        when(orderCancelService.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(orderCancelService.loadForCancel(any())).thenReturn(context);
        when(paymentCancelUseCase.cancelOnPg(eq(ORDER_ID), anyLong(), anyString()))
                .thenReturn(Optional.of(new PaymentCancelReceipt(
                        CANCEL_TOTAL_AMOUNT, CancelReasonCode.CUSTOMER_CANCEL.name(), Instant.now(), "toss-tx-key")));
        when(orderCancelService.applyCancel(any(), any(), any())).thenReturn(result());
    }

    private OrderCancelContext context(UUID productId, UUID timeDealId) {
        return new OrderCancelContext(
                ORDER_ID, 15_000L, 3_000L, CANCEL_TOTAL_AMOUNT,
                List.of(new OrderCancelContext.CancelTargetItem(ITEM_ID, productId, timeDealId)));
    }

    private CancelOrderCommand command() {
        return new CancelOrderCommand(
                ORDER_ID, List.of(ITEM_ID), CancelReasonCode.CUSTOMER_CANCEL, null,
                CanceledByType.CUSTOMER, CUSTOMER_ID, IDEMPOTENCY_KEY);
    }

    private OrderCancelResult result() {
        return new OrderCancelResult(
                UUID.randomUUID(), ORDER_ID, CANCEL_TOTAL_AMOUNT, CancelReasonCode.CUSTOMER_CANCEL,
                CanceledByType.CUSTOMER, 15_000L, 3_000L, CANCEL_TOTAL_AMOUNT, true, Instant.now(),
                List.of(), null);
    }
}
