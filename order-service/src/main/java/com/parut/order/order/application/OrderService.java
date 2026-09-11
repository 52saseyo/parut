package com.parut.order.order.application;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.dto.CreateOrderCommand;
import com.parut.order.order.application.dto.CreateTimeDealOrderCommand;
import com.parut.order.order.application.dto.CreatedOrder;
import com.parut.order.order.application.dto.OrderDetailData;
import com.parut.order.order.application.port.out.dto.ProductOrderInfo;
import com.parut.order.order.application.port.out.dto.TimeDealInfo;
import com.parut.order.order.domain.Order;
import com.parut.order.order.domain.OrderCancel;
import com.parut.order.order.domain.OrderDeliveryGroup;
import com.parut.order.order.domain.OrderItem;
import com.parut.order.order.domain.OrderStatus;
import com.parut.order.order.domain.OrderStatusHistory;
import com.parut.order.order.domain.OrderType;
import com.parut.order.order.infrastructure.persistence.OrderCancelRepository;
import com.parut.order.order.infrastructure.persistence.OrderDeliveryGroupRepository;
import com.parut.order.order.infrastructure.persistence.OrderItemRepository;
import com.parut.order.order.infrastructure.persistence.OrderRepository;
import com.parut.order.order.infrastructure.persistence.OrderStatusHistoryRepository;
import com.parut.order.payment.application.port.in.PaymentQueryUseCase;
import com.parut.order.payment.application.port.in.dto.PaymentView;

import lombok.RequiredArgsConstructor;

/**
 * Order 도메인의 DB 상태 변경과 조회를 전담합니다.
 *
 * <p>외부 API(Feign) 호출 흐름은 {@link OrderFacade}가 제어하며,
 * 이 클래스는 순수 DB I/O를 위한 짧은 단위 트랜잭션만 제공합니다.
 *
 * <p>주문 상세 조회 시 필요한 결제 정보는 {@link PaymentQueryUseCase}를 통해 조합합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    static final long DELIVERY_FEE_PER_SELLER = 3_000L; // ToDo: 배송비 정책 구체화 시점에 수정
    static final Duration ORDER_TTL = Duration.ofHours(24);
    private static final String ADMIN_ROLE = "ADMIN";
    private static final DateTimeFormatter ORDER_NO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OrderRepository orderRepository;
    private final OrderDeliveryGroupRepository orderDeliveryGroupRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OrderCancelRepository orderCancelRepository;
    private final PaymentQueryUseCase paymentQueryUseCase;

    public Optional<Order> findByIdempotencyKey(String idempotencyKey) {
        return orderRepository.findByIdempotencyKey(idempotencyKey);
    }

    /**
     * 주문 초기 데이터(주문, 배송그룹, 아이템, 이력)를 DB에 확정 저장합니다.
     *
     * <p>동시 요청으로 멱등키가 충돌할 경우 예외를 직접 처리하지 않고 ({@link OrderFacade})로 전파하여,
     * 해당 트랜잭션이 깔끔하게 롤백되도록 유도합니다.
     */
    // ToDo: bulk 도입 시 수정 예정
    @Transactional
    public CreatedOrder saveNewOrder(CreateOrderCommand command, ProductOrderInfo productInfo) {
        // 일반 상품은 할인이 없어 unitPrice = originalPrice 로 저장
        // 타임딜 주문은 unitPrice에 할인가 저장
        long unitPrice = productInfo.originalPrice();
        long totalProductAmount = unitPrice * command.quantity();

        Order order = orderRepository.saveAndFlush(buildOrder(command, totalProductAmount));
        recordHistory(order.getId(), null, OrderStatus.CREATED, "주문 생성", command.userId());

        OrderDeliveryGroup group = orderDeliveryGroupRepository.save(
                OrderDeliveryGroup.create(order.getId(), productInfo.sellerId(), totalProductAmount, DELIVERY_FEE_PER_SELLER)
        );
        OrderItem item = orderItemRepository.save(
                OrderItem.create(
                        order.getId(),
                        group.getId(),
                        productInfo.productId(),
                        null,
                        productInfo.productName(),
                        productInfo.appearanceType(),
                        productInfo.origin(),
                        productInfo.harvestDate(),
                        productInfo.saleUnit(),
                        productInfo.unitQuantity(),
                        productInfo.originalPrice(),
                        unitPrice,
                        command.quantity()
                )
        );

        return new CreatedOrder(order, item);
    }

    /**
     * 타임딜 주문 초기 데이터를 DB에 확정 저장합니다.
     * unitPrice는 정가가 아닌 dealPrice(할인가)로 저장합니다.
     */
    @Transactional
    public CreatedOrder saveNewTimeDealOrder(CreateTimeDealOrderCommand command, TimeDealInfo timeDealInfo) {
        long unitPrice = timeDealInfo.dealPrice();
        long totalProductAmount = unitPrice * command.quantity();

        Order order = orderRepository.saveAndFlush(buildTimeDealOrder(command, totalProductAmount));
        recordHistory(order.getId(), null, OrderStatus.CREATED, "주문 생성", command.userId());

        OrderDeliveryGroup group = orderDeliveryGroupRepository.save(
                OrderDeliveryGroup.create(order.getId(), timeDealInfo.sellerId(), totalProductAmount, DELIVERY_FEE_PER_SELLER)
        );
        OrderItem item = orderItemRepository.save(
                OrderItem.create(
                        order.getId(),
                        group.getId(),
                        timeDealInfo.productId(),
                        timeDealInfo.timeDealId(),
                        timeDealInfo.productName(),
                        timeDealInfo.productGrade(),
                        timeDealInfo.origin(),
                        timeDealInfo.harvestedDate(),
                        null,
                        null,
                        timeDealInfo.originalPrice(),
                        unitPrice,
                        command.quantity()
                )
        );

        return new CreatedOrder(order, item);
    }

    @Transactional
    public Order markStockReserved(UUID orderId, UUID actorId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // 미결제 주문 정리용 TTL
        // 재고 예약의 만료, 해제는 Product가 전적으로 처리하며 Order는 관여 x
        order.markStockReserved(Instant.now().plus(ORDER_TTL));

        orderRepository.saveAndFlush(order);
        recordHistory(orderId, OrderStatus.CREATED, OrderStatus.STOCK_RESERVED, "재고 예약 성공", actorId);

        return order;
    }

    /**
     * 재고 예약 실패 시 호출되는 보상 트랜잭션입니다.
     *
     * <p>{@link #saveNewOrder}로 저장된 주문 관련 데이터를 모두 삭제합니다.
     * 이를 통해 점유되었던 멱등키를 해제하여 클라이언트가 재시도할 수 있도록 원상 복구합니다.
     */
    @Transactional
    public void deleteFailedOrder(UUID orderId) {
        orderStatusHistoryRepository.deleteByOrderId(orderId);
        orderItemRepository.deleteByOrderId(orderId);
        orderDeliveryGroupRepository.deleteByOrderId(orderId);
        orderRepository.deleteById(orderId);
    }

    private Order buildOrder(CreateOrderCommand command, long totalProductAmount) {
        return Order.create(
                generateOrderNo(),
                command.userId(),
                OrderType.NORMAL,
                command.recipientName(),
                command.recipientPhone(),
                command.zipCode(),
                command.addressBase(),
                command.addressDetail(),
                command.deliveryRequest(),
                totalProductAmount,
                DELIVERY_FEE_PER_SELLER,
                command.idempotencyKey()
        );
    }

    private Order buildTimeDealOrder(CreateTimeDealOrderCommand command, long totalProductAmount) {
        return Order.create(
                generateOrderNo(),
                command.userId(),
                OrderType.TIME_DEAL,
                command.recipientName(),
                command.recipientPhone(),
                command.zipCode(),
                command.addressBase(),
                command.addressDetail(),
                command.deliveryRequest(),
                totalProductAmount,
                DELIVERY_FEE_PER_SELLER,
                command.idempotencyKey()
        );
    }

    private void recordHistory(UUID orderId, OrderStatus from, OrderStatus to, String reason, UUID actorId) {
        orderStatusHistoryRepository.save(
                OrderStatusHistory.record(orderId, from, to, reason, actorId.toString())
        );
    }

    private String generateOrderNo() {
        String date = LocalDate.now().format(ORDER_NO_DATE_FORMAT);
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "ORD-" + date + "-" + random;
    }

    public OrderDetailData getOrderDetail(UUID orderId, UUID requesterId, String requesterRole) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!ADMIN_ROLE.equals(requesterRole) && !order.getUserId().equals(requesterId)) {
            throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        List<OrderDeliveryGroup> groups = orderDeliveryGroupRepository.findByOrderId(orderId);
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        PaymentView payment = paymentQueryUseCase.getPayment(orderId).orElse(null);
        List<OrderCancel> cancels = orderCancelRepository.findByOrderId(orderId);

        return OrderDetailData.from(order, groups, items, payment, cancels);
    }
}
