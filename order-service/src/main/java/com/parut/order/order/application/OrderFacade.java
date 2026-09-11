package com.parut.order.order.application;

import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.dto.CreateOrderCommand;
import com.parut.order.order.application.dto.CreateTimeDealOrderCommand;
import com.parut.order.order.application.dto.CreatedOrder;
import com.parut.order.order.application.port.out.ProductClient;
import com.parut.order.order.application.port.out.TimeDealClient;
import com.parut.order.order.application.port.out.dto.ProductOrderInfo;
import com.parut.order.order.application.port.out.dto.TimeDealInfo;
import com.parut.order.order.domain.Order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 주문 생성의 전체 흐름을 조율합니다.
 *
 * <p>DB 커넥션 점유 시간을 최소화하기 위해 자체적인 트랜잭션을 열지 않으며,
 * 외부 API(Feign) 호출과 {@link OrderService}의 단위 트랜잭션을 분리하여 순차적으로 실행합니다.
 */
// ToDo: bulk 도입 시 수정 예정
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderService orderService;
    private final ProductClient productClient;
    private final TimeDealClient timeDealClient;

    public Order createOrder(CreateOrderCommand command) {
        return orderService.findByIdempotencyKey(command.idempotencyKey())
                .orElseGet(() -> createNewOrder(command));
    }

    private Order createNewOrder(CreateOrderCommand command) {
        ProductOrderInfo productInfo = productClient.getOrderInfo(command.productId());
        if (!productInfo.purchasable()) {
            throw new BusinessException(ErrorCode.PRODUCT_UNAVAILABLE);
        }

        CreatedOrder created;
        try {
            created = orderService.saveNewOrder(command, productInfo);
        } catch (DataIntegrityViolationException e) {
            // 멱등키 충돌(동시 요청)시, 앞선 요청으로 이미 생성된 주문 반환
            return orderService.findByIdempotencyKey(command.idempotencyKey())
                    .orElseThrow(() -> new BusinessException(ErrorCode.DUPLICATE_ORDER_REQUEST));
        }

        UUID orderId = created.order().getId();
        UUID itemId = created.item().getId();

        try {
            productClient.reserveStock(command.productId(), orderId, itemId, command.quantity());
        } catch (RuntimeException e) {
            // 재고 예약 실패(품절 등)시, 선 저장된 주문과 멱등키를 삭제하여 롤백 (클라이언트 재요청 가능하도록 원복)
            orderService.deleteFailedOrder(orderId);
            throw e;
        }

        try {
            return orderService.markStockReserved(orderId, command.userId());
        } catch (DataAccessException e) {
            // 주문 저장 실패시, 예약된 재고 해제 (보상 트랜잭션)
            log.error("[OrderFacade] 주문 저장 실패로 재고 예약을 해제합니다. orderId={}", orderId, e);
            safelyRestoreStock(command.productId(), orderId, itemId);
            throw e;
        }
    }

    private void safelyRestoreStock(UUID productId, UUID orderId, UUID orderItemId) {
        try {
            productClient.restoreStock(productId, orderId, orderItemId);
        } catch (RuntimeException e) {
            // 재고 복원 요청 실패 케이스 고려
            log.error("[OrderFacade] 재고 해제 실패. 수동 대응 필요 productId={}, orderId={}, orderItemId={}",
                    productId, orderId, orderItemId, e);
        }
    }

    public Order createTimeDealOrder(CreateTimeDealOrderCommand command) {
        return orderService.findByIdempotencyKey(command.idempotencyKey())
                .orElseGet(() -> createNewTimeDealOrder(command));
    }

    private Order createNewTimeDealOrder(CreateTimeDealOrderCommand command) {
        TimeDealInfo timeDealInfo = timeDealClient.getOrderInfo(command.timeDealId());

        CreatedOrder created;
        try {
            created = orderService.saveNewTimeDealOrder(command, timeDealInfo);
        } catch (DataIntegrityViolationException e) {
            // 멱등키 충돌(동시 요청)시, 앞선 요청으로 이미 생성된 주문 반환
            return orderService.findByIdempotencyKey(command.idempotencyKey())
                    .orElseThrow(() -> new BusinessException(ErrorCode.DUPLICATE_ORDER_REQUEST));
        }

        UUID orderId = created.order().getId();

        try {
            timeDealClient.reserveStock(command.timeDealId(), orderId, command.userId(), command.quantity());
        } catch (RuntimeException e) {
            // 재고 예약 실패(품절 등)시, 선 저장된 주문과 멱등키를 삭제하여 롤백 (클라이언트 재요청 가능하도록 원복)
            orderService.deleteFailedOrder(orderId);
            throw e;
        }

        try {
            return orderService.markStockReserved(orderId, command.userId());
        } catch (DataAccessException e) {
            // 주문 저장 실패시, 예약된 재고 해제 (보상 트랜잭션)
            log.error("[OrderFacade] 타임딜 주문 저장 실패로 재고 예약을 해제합니다. orderId={}", orderId, e);
            safelyRestoreTimeDealStock(orderId);
            throw e;
        }
    }

    private void safelyRestoreTimeDealStock(UUID orderId) {
        try {
            timeDealClient.restoreStock(orderId, null);
        } catch (RuntimeException e) {
            // 재고 복원 요청 실패 케이스 고려
            log.error("[OrderFacade] 타임딜 재고 해제 실패. 수동 대응 필요 orderId={}", orderId, e);
        }
    }
}
