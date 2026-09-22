package com.parut.order.order.application;

import com.parut.order.global.auth.UserRole;
import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.dto.*;
import com.parut.order.order.application.port.out.dto.ProductOrderInfo;
import com.parut.order.order.application.port.out.dto.TimeDealInfo;
import com.parut.order.order.domain.*;
import com.parut.order.order.infrastructure.persistence.*;
import com.parut.order.payment.application.port.in.PaymentQueryUseCase;
import com.parut.order.payment.application.port.in.dto.PaymentView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

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
    static final int MAX_SELLER_COUNT = 5;
    static final Duration ORDER_TTL = Duration.ofHours(24);
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
     * <p>아이템을 판매자(sellerId) 기준으로 그룹핑해 배송그룹을 판매자 수만큼 생성합니다.
     *
     * <p>동시 요청으로 멱등키가 충돌할 경우 예외를 직접 처리하지 않고 ({@link OrderFacade})로 전파하여,
     * 해당 트랜잭션이 깔끔하게 롤백되도록 유도합니다.
     */
    @Transactional
    public CreatedOrder saveNewOrder(CreateOrderCommand command, Map<UUID, ProductOrderInfo> productInfoByProductId) {
        // 일반 상품은 할인이 없어 unitPrice = originalPrice 로 저장
        long totalProductAmount = command.items().stream()
                .mapToLong(item -> productInfoByProductId.get(item.productId()).originalPrice() * item.quantity())
                .sum();

        Set<UUID> sellerIds = command.items().stream()
                .map(item -> productInfoByProductId.get(item.productId()).sellerId())
                .collect(Collectors.toSet());
        if (sellerIds.size() > MAX_SELLER_COUNT) {
            throw new BusinessException(ErrorCode.TOO_MANY_SELLERS);
        }
        long totalDeliveryFee = DELIVERY_FEE_PER_SELLER * sellerIds.size();

        Order order = orderRepository.saveAndFlush(buildOrder(command, totalProductAmount, totalDeliveryFee));
        recordHistory(order.getId(), null, OrderStatus.CREATED, "주문 생성", command.userId());

        Map<UUID, List<OrderItemCommand>> itemsBySeller = command.items().stream()
                .collect(Collectors.groupingBy(item -> productInfoByProductId.get(item.productId()).sellerId()));

        List<OrderItem> savedItems = new ArrayList<>();
        for (Map.Entry<UUID, List<OrderItemCommand>> entry : itemsBySeller.entrySet()) {
            UUID sellerId = entry.getKey();
            List<OrderItemCommand> sellerItems = entry.getValue();
            long groupProductAmount = sellerItems.stream()
                    .mapToLong(item -> productInfoByProductId.get(item.productId()).originalPrice() * item.quantity())
                    .sum();

            OrderDeliveryGroup group = orderDeliveryGroupRepository.save(
                    OrderDeliveryGroup.create(order.getId(), sellerId, groupProductAmount, DELIVERY_FEE_PER_SELLER)
            );

            for (OrderItemCommand item : sellerItems) {
                ProductOrderInfo productInfo = productInfoByProductId.get(item.productId());
                long unitPrice = productInfo.originalPrice();
                savedItems.add(orderItemRepository.save(
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
                                item.quantity()
                        )
                ));
            }
        }

        return new CreatedOrder(order, savedItems);
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

        return new CreatedOrder(order, List.of(item));
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

    private Order buildOrder(CreateOrderCommand command, long totalProductAmount, long totalDeliveryFee) {
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
                totalDeliveryFee,
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

    public OrderDetailData getOrderDetail(UUID orderId, UUID requesterId, UserRole requesterRole) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (requesterRole != UserRole.ADMIN && !order.getUserId().equals(requesterId)) {
            throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        List<OrderDeliveryGroup> groups = orderDeliveryGroupRepository.findByOrderId(orderId);
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        PaymentView payment = paymentQueryUseCase.getPayment(orderId).orElse(null);
        List<OrderCancel> cancels = orderCancelRepository.findByOrderId(orderId);

        return OrderDetailData.from(order, groups, items, payment, cancels);
    }

    public OrderItemPage getBuyerOrderItems(
            UUID userId,
            OrderItemStatus itemStatus,
            OrderStatus orderStatus,
            OrderType orderType,
            Instant startDate,
            Instant endDate,
            String cursor,
            UUID cursorId,
            int size
    ) {
        validateOrderItemListQuery(userId, startDate, endDate, cursor, cursorId, size);
        Instant cursorTime = parseCursor(cursor);

        PageRequest pageable = PageRequest.of(0, size + 1);
        List<OrderItem> items = orderItemRepository.findBuyerOrderItems(
                userId, itemStatus, orderStatus, orderType,
                startDate == null ? Instant.EPOCH : startDate,
                endDate == null ? Instant.now().plusSeconds(60) : endDate,
                CancelReasonCode.SYSTEM_TIMEOUT,
                cursorTime == null ? Instant.now().plusSeconds(60) : cursorTime,
                cursorId, pageable
        );

        boolean hasNext = items.size() > size;
        List<OrderItem> content = hasNext ? items.subList(0, size) : items;
        List<OrderItemSummary> summaries = toSummaries(content);

        if (!hasNext) {
            return new OrderItemPage(summaries, null, null, false);
        }

        OrderItem lastItem = content.getLast();
        return new OrderItemPage(summaries, lastItem.getCreatedAt().toString(), lastItem.getId(), true);
    }

    private List<OrderItemSummary> toSummaries(List<OrderItem> items) {
        if (items.isEmpty()) {
            return List.of();
        }

        Map<UUID, Order> ordersById = orderRepository.findAllById(
                items.stream().map(OrderItem::getOrderId).distinct().toList()
        ).stream().collect(Collectors.toMap(Order::getId, order -> order));

        Map<UUID, OrderDeliveryGroup> groupsById = orderDeliveryGroupRepository.findAllById(
                items.stream().map(OrderItem::getDeliveryGroupId).distinct().toList()
        ).stream().collect(Collectors.toMap(OrderDeliveryGroup::getId, group -> group));

        return items.stream()
                .map(item -> OrderItemSummary.from(
                        item,
                        ordersById.get(item.getOrderId()),
                        groupsById.get(item.getDeliveryGroupId())
                ))
                .toList();
    }

    private void validateOrderItemListQuery(
            UUID userId,
            Instant startDate,
            Instant endDate,
            String cursor,
            UUID cursorId,
            int size
    ) {
        boolean hasOnlyOneCursorValue = (cursor == null) != (cursorId == null);
        if (userId == null || hasOnlyOneCursorValue) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (size != 10 && size != 30 && size != 50) {
            throw new BusinessException(ErrorCode.INVALID_PAGE_SIZE);
        }
        if (startDate != null && endDate != null
                && (endDate.isBefore(startDate) || Duration.between(startDate, endDate).toDays() > 365)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private Instant parseCursor(String cursor) {
        if (cursor == null) {
            return null;
        }
        try {
            return Instant.parse(cursor);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
