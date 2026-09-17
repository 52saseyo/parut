package com.parut.order.order.application;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.dto.CancelOrderCommand;
import com.parut.order.order.application.dto.OrderCancelContext;
import com.parut.order.order.application.dto.OrderCancelResult;
import com.parut.order.order.application.port.out.ProductClient;
import com.parut.order.order.application.port.out.TimeDealClient;
import com.parut.order.payment.application.port.in.PaymentCancelUseCase;
import com.parut.order.payment.application.port.in.dto.PaymentCancelReceipt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 주문 취소의 전체 흐름을 조율합니다.
 *
 * <p>PG 취소와 재고 복원(Feign)은 트랜잭션 밖에서 수행하고,
 * DB 갱신만 {@link OrderCancelService}의 단일 트랜잭션에 맡깁니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelFacade {

    private final OrderCancelService orderCancelService;
    private final PaymentCancelUseCase paymentCancelUseCase;
    private final ProductClient productClient;
    private final TimeDealClient timeDealClient;

    public OrderCancelResult cancel(CancelOrderCommand command) {
        return orderCancelService.findByIdempotencyKey(command.idempotencyKey())
                .orElseGet(() -> cancelNewRequest(command));
    }

    private OrderCancelResult cancelNewRequest(CancelOrderCommand command) {
        OrderCancelContext context = orderCancelService.loadForCancel(command);

        PaymentCancelReceipt receipt = paymentCancelUseCase
                .cancelOnPg(context.orderId(), context.cancelTotalAmount(), command.cancelReasonCode().name())
                .orElse(null);

        OrderCancelResult result;
        try {
            result = orderCancelService.applyCancel(command, context, receipt);
        } catch (DataIntegrityViolationException e) {
            // 멱등키 충돌(동시 요청)시, 앞선 요청으로 이미 처리된 취소 반환
            return orderCancelService.findByIdempotencyKey(command.idempotencyKey())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CONCURRENT_MODIFICATION));
        }

        restoreStock(command, context);

        return result;
    }

    // ToDo: bulk 도입 시 아이템별 반복 호출을 1회로 개선 예정
    private void restoreStock(CancelOrderCommand command, OrderCancelContext context) {
        context.items().stream()
                .filter(item -> item.timeDealId() == null)
                .forEach(item -> safelyRestore(
                        () -> productClient.restoreStock(item.productId(), context.orderId(), item.orderItemId()),
                        context.orderId()));

        // 타임딜 재고 해제는 orderId 단위 API라 아이템 수와 무관하게 한 번만 호출한다.
        if (context.items().stream().anyMatch(item -> item.timeDealId() != null)) {
            safelyRestore(
                    () -> timeDealClient.restoreStock(context.orderId(), command.cancelReasonCode().name()),
                    context.orderId());
        }
    }

    // 취소와 환불은 이미 확정됐으므로 복원 실패로 되돌리지 않고 로그만 남긴다.
    private void safelyRestore(Runnable restoreStock, UUID orderId) {
        try {
            restoreStock.run();
        } catch (RuntimeException e) {
            log.error("[OrderCancelFacade] 재고 복원 실패. 수동 대응 필요 orderId={}", orderId, e);
        }
    }
}
