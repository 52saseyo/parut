package com.parut.order.payment.application;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.domain.OrderStatus;
import com.parut.order.payment.application.dto.PaymentConfirmCommand;
import com.parut.order.payment.application.dto.PaymentConfirmContext;
import com.parut.order.payment.application.dto.PaymentConfirmResult;
import com.parut.order.payment.application.port.out.PaymentGateway;
import com.parut.order.payment.application.port.out.ProductStockConfirmClient;
import com.parut.order.payment.application.port.out.dto.PaymentApproveResult;
import com.parut.order.payment.application.port.out.dto.PaymentCancelResult;
import com.parut.order.payment.domain.PaymentMethod;
import com.parut.order.payment.domain.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentFacadeTest {

    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ORDER_ITEM_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final long AMOUNT = 33_000L;

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private ProductStockConfirmClient productStockConfirmClient;

    @InjectMocks
    private PaymentFacade paymentFacade;

    private PaymentConfirmCommand command() {
        return new PaymentConfirmCommand("payment-key-1", "ORD-20260908-AAAAAAAA", AMOUNT, "idem-confirm-0001");
    }

    private PaymentConfirmContext context() {
        return new PaymentConfirmContext(PAYMENT_ID, ORDER_ID, USER_ID, ORDER_ITEM_ID, PRODUCT_ID);
    }

    private PaymentApproveResult approveResult() {
        return new PaymentApproveResult(PaymentMethod.CREDIT_CARD, Instant.now(), "receipt-url", "pg-tx-approve-1");
    }

    private PaymentConfirmResult confirmResult(PaymentApproveResult approveResult, PaymentConfirmCommand command) {
        return new PaymentConfirmResult(
                PAYMENT_ID, ORDER_ID, command.tossOrderId(), PaymentStatus.DONE, PaymentMethod.CREDIT_CARD,
                AMOUNT, AMOUNT, approveResult.approvedAt(), approveResult.receiptUrl(), OrderStatus.PAID
        );
    }

    @Test
    @DisplayName("결제 승인: 재고 확정까지 성공하면 배송 준비로 넘어가고 PAID 결과를 반환한다")
    void 결제승인_성공() {
        PaymentConfirmCommand command = command();
        PaymentConfirmContext context = context();
        PaymentApproveResult approveResult = approveResult();
        PaymentConfirmResult confirmResult = confirmResult(approveResult, command);

        when(paymentService.loadForConfirm(command)).thenReturn(context);
        when(paymentGateway.approve(command.paymentKey(), command.tossOrderId(), command.amount(), command.idempotencyKey()))
                .thenReturn(approveResult);
        when(paymentService.applyApproved(context, command, approveResult)).thenReturn(confirmResult);

        PaymentConfirmResult result = paymentFacade.confirm(command);

        assertThat(result.orderStatus()).isEqualTo(OrderStatus.PAID);
        verify(productStockConfirmClient).confirmStock(PRODUCT_ID, ORDER_ID, ORDER_ITEM_ID);
        verify(paymentService).markDeliveryPreparing(ORDER_ID);
        verify(paymentGateway, never()).cancel(any(), anyLong(), any());
        verify(paymentService, never()).applyStockShortageCancel(any(), any());
    }

    @Test
    @DisplayName("결제 승인: PG 승인 자체가 실패하면 결제를 중단 처리하고 PG_APPROVE_FAILED를 던진다")
    void 결제승인_PG승인실패_중단처리() {
        PaymentConfirmCommand command = command();
        PaymentConfirmContext context = context();

        when(paymentService.loadForConfirm(command)).thenReturn(context);
        doThrow(new RuntimeException("PG 통신 실패"))
                .when(paymentGateway).approve(command.paymentKey(), command.tossOrderId(), command.amount(), command.idempotencyKey());

        assertThatThrownBy(() -> paymentFacade.confirm(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PG_APPROVE_FAILED);

        verify(paymentService).applyAborted(context);
        verifyNoInteractions(productStockConfirmClient);
        verify(paymentService, never()).applyApproved(any(), any(), any());
        verify(paymentService, never()).markDeliveryPreparing(any());
    }

    @Test
    @DisplayName("결제 승인: 재고 확정 실패가 STOCK_SHORTAGE가 아니면 보상 없이 그대로 전파한다")
    void 결제승인_재고확정실패_다른에러코드는_보상없이_전파() {
        PaymentConfirmCommand command = command();
        PaymentConfirmContext context = context();
        PaymentApproveResult approveResult = approveResult();
        PaymentConfirmResult confirmResult = confirmResult(approveResult, command);

        when(paymentService.loadForConfirm(command)).thenReturn(context);
        when(paymentGateway.approve(command.paymentKey(), command.tossOrderId(), command.amount(), command.idempotencyKey()))
                .thenReturn(approveResult);
        when(paymentService.applyApproved(context, command, approveResult)).thenReturn(confirmResult);
        doThrow(new BusinessException(ErrorCode.PRODUCT_UNAVAILABLE))
                .when(productStockConfirmClient).confirmStock(PRODUCT_ID, ORDER_ID, ORDER_ITEM_ID);

        assertThatThrownBy(() -> paymentFacade.confirm(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_UNAVAILABLE);

        verify(paymentGateway, never()).cancel(any(), anyLong(), any());
        verify(paymentService, never()).applyStockShortageCancel(any(), any());
        verify(paymentService, never()).markDeliveryPreparing(any());
    }

    @Test
    @DisplayName("결제 승인: 재고 확정이 실패하면 PG 취소 후 주문을 취소하고 STOCK_SHORTAGE를 던진다")
    void 결제승인_재고확정실패_보상처리() {
        PaymentConfirmCommand command = command();
        PaymentConfirmContext context = context();
        PaymentApproveResult approveResult = approveResult();
        PaymentConfirmResult confirmResult = confirmResult(approveResult, command);
        PaymentCancelResult cancelResult = new PaymentCancelResult(Instant.now(), "pg-tx-cancel-1");

        when(paymentService.loadForConfirm(command)).thenReturn(context);
        when(paymentGateway.approve(command.paymentKey(), command.tossOrderId(), command.amount(), command.idempotencyKey()))
                .thenReturn(approveResult);
        when(paymentService.applyApproved(context, command, approveResult)).thenReturn(confirmResult);
        doThrow(new BusinessException(ErrorCode.STOCK_SHORTAGE))
                .when(productStockConfirmClient).confirmStock(PRODUCT_ID, ORDER_ID, ORDER_ITEM_ID);
        when(paymentGateway.cancel(command.paymentKey(), confirmResult.balanceAmount(), "OUT_OF_STOCK"))
                .thenReturn(cancelResult);

        assertThatThrownBy(() -> paymentFacade.confirm(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.STOCK_SHORTAGE);

        verify(paymentGateway).cancel(command.paymentKey(), confirmResult.balanceAmount(), "OUT_OF_STOCK");
        verify(paymentService).applyStockShortageCancel(context, cancelResult);
        verify(paymentService, never()).markDeliveryPreparing(any());
    }

    @Test
    @DisplayName("결제 승인: 재고 확정 실패 후 PG 취소마저 실패하면 상태 변경 없이 PG_CANCEL_FAILED를 던진다")
    void 결제승인_재고확정실패_PG취소도실패() {
        PaymentConfirmCommand command = command();
        PaymentConfirmContext context = context();
        PaymentApproveResult approveResult = approveResult();
        PaymentConfirmResult confirmResult = confirmResult(approveResult, command);

        when(paymentService.loadForConfirm(command)).thenReturn(context);
        when(paymentGateway.approve(command.paymentKey(), command.tossOrderId(), command.amount(), command.idempotencyKey()))
                .thenReturn(approveResult);
        when(paymentService.applyApproved(context, command, approveResult)).thenReturn(confirmResult);
        doThrow(new BusinessException(ErrorCode.STOCK_SHORTAGE))
                .when(productStockConfirmClient).confirmStock(PRODUCT_ID, ORDER_ID, ORDER_ITEM_ID);
        doThrow(new RuntimeException("PG down"))
                .when(paymentGateway).cancel(command.paymentKey(), confirmResult.balanceAmount(), "OUT_OF_STOCK");

        assertThatThrownBy(() -> paymentFacade.confirm(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PG_CANCEL_FAILED);

        verify(paymentService, never()).applyStockShortageCancel(any(), any());
    }
}
