package com.parut.order.refund.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.parut.order.payment.application.port.in.PaymentCancelUseCase;
import com.parut.order.payment.application.port.in.dto.PaymentCancelCommand;
import com.parut.order.payment.application.port.in.dto.PaymentCancelView;
import com.parut.order.refund.application.dto.RefundApprovalContext;
import com.parut.order.refund.domain.Refund;

@ExtendWith(MockitoExtension.class)
class RefundFacadeTest {

    @Mock
    private RefundService refundService;

    @Mock
    private PaymentCancelUseCase paymentCancelUseCase;

    @InjectMocks
    private RefundFacade refundFacade;

    @Test
    @DisplayName("같은 주문의 환불 요청 여러 건을 합산해 결제를 한 번 취소한다")
    void 환불_다건_결제_취소() {
        UUID firstRefundId = UUID.randomUUID();
        UUID secondRefundId = UUID.randomUUID();
        UUID firstOrderItemId = UUID.randomUUID();
        UUID secondOrderItemId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        List<UUID> refundIds = List.of(firstRefundId, secondRefundId);
        RefundApprovalContext context = new RefundApprovalContext(
                orderId,
                sellerId,
                refundIds,
                List.of(firstOrderItemId, secondOrderItemId),
                20_000L
        );
        PaymentCancelView paymentCancel = new PaymentCancelView(20_000L, Instant.now());
        List<Refund> approvedRefunds = List.of(
                Refund.request(firstOrderItemId, 10_000L, "상품 불량", Instant.now()),
                Refund.request(secondOrderItemId, 10_000L, "상품 파손", Instant.now())
        );

        when(refundService.prepareApproval(refundIds, sellerId)).thenReturn(context);
        when(paymentCancelUseCase.cancel(org.mockito.ArgumentMatchers.any(PaymentCancelCommand.class)))
                .thenReturn(paymentCancel);
        when(refundService.completeApproval(context, paymentCancel)).thenReturn(approvedRefunds);

        List<Refund> result = refundFacade.approveRefunds(refundIds, sellerId, "refund-approval-1");

        ArgumentCaptor<PaymentCancelCommand> commandCaptor = ArgumentCaptor.forClass(PaymentCancelCommand.class);
        verify(paymentCancelUseCase, times(1)).cancel(commandCaptor.capture());
        assertThat(commandCaptor.getValue().orderId()).isEqualTo(orderId);
        assertThat(commandCaptor.getValue().cancelRequestId()).isEqualTo("refund-approval-1");
        assertThat(commandCaptor.getValue().cancelAmount()).isEqualTo(20_000L);
        assertThat(commandCaptor.getValue().reason()).isEqualTo("REFUND");
        assertThat(result).isSameAs(approvedRefunds);
    }
}
