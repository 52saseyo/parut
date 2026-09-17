package com.parut.order.order.application;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.dto.CancelOrderCommand;
import com.parut.order.order.application.dto.OrderCancelContext;
import com.parut.order.order.application.dto.OrderCancelResult;
import com.parut.order.order.application.port.in.OrderCancelUseCase;
import com.parut.order.order.domain.*;
import com.parut.order.order.infrastructure.persistence.OrderCancelRepository;
import com.parut.order.order.infrastructure.persistence.OrderDeliveryGroupRepository;
import com.parut.order.order.infrastructure.persistence.OrderItemRepository;
import com.parut.order.order.infrastructure.persistence.OrderRepository;
import com.parut.order.payment.application.port.in.PaymentCancelUseCase;
import com.parut.order.payment.application.port.in.PaymentQueryUseCase;
import com.parut.order.payment.application.port.in.dto.PaymentCancelReceipt;
import com.parut.order.payment.application.port.in.dto.PaymentCancelView;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderCancelService implements OrderCancelUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderDeliveryGroupRepository orderDeliveryGroupRepository;
    private final OrderCancelRepository orderCancelRepository;
    private final PaymentCancelUseCase paymentCancelUseCase;
    private final PaymentQueryUseCase paymentQueryUseCase;

    @Value("${app.system-account-id}")
    private String systemAccountId;

    @Override
    @Transactional
    public void cancelForStockShortage(UUID orderId) {
        // 락 획득 순서: 주문 -> 배송그룹 -> 아이템
        Order order = getOrder(orderId);
        List<OrderDeliveryGroup> groups = orderDeliveryGroupRepository.findByOrderId(orderId);
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

        long cancelProductAmount = items.stream().mapToLong(item -> item.getUnitPrice() * item.getQuantity()).sum();
        long cancelDeliveryFee = groups.stream().mapToLong(OrderDeliveryGroup::getDeliveryFee).sum(); // 그룹에 ORDERED 아이템이 안 남으므로 전액 환불

        OrderCancel orderCancel = orderCancelRepository.save(
                OrderCancel.create(
                        orderId,
                        CancelReasonCode.OUT_OF_STOCK,
                        null,
                        CanceledByType.SYSTEM,
                        systemAccountId,
                        cancelProductAmount,
                        cancelDeliveryFee,
                        true,
                        UUID.randomUUID().toString()
                )
        );

        Instant canceledAt = Instant.now();
        groups.forEach(group -> group.cancel(canceledAt));
        items.forEach(item -> item.cancel(orderCancel.getId()));
        order.applyCancellation(orderCancel.getCancelTotalAmount());
    }

    public Optional<OrderCancelResult> findByIdempotencyKey(String idempotencyKey) {
        return orderCancelRepository.findByIdempotencyKey(idempotencyKey)
                .map(orderCancel -> OrderCancelResult.of(
                        orderCancel,
                        getOrder(orderCancel.getOrderId()).getCanceledAmount(),
                        orderItemRepository.findByCancelId(orderCancel.getId()),
                        paymentSummary(orderCancel.getOrderId())
                ));
    }

    public OrderCancelContext loadForCancel(CancelOrderCommand command) {
        Order order = getOrder(command.orderId());
        Map<UUID, OrderDeliveryGroup> groups = groupsOf(order.getId());
        List<OrderItem> allItems = orderItemRepository.findByOrderId(order.getId());
        List<OrderItem> targets = targetsOf(allItems, command.orderItemIds());

        verifyOwnership(order, targets, groups, command);
        verifyCancelable(targets, groups);

        long cancelProductAmount = targets.stream()
                .mapToLong(item -> item.getUnitPrice() * item.getQuantity())
                .sum();
        long cancelDeliveryFee = fullyCanceledGroupIds(allItems, targets).stream()
                .mapToLong(groupId -> groups.get(groupId).getDeliveryFee())
                .sum();

        return new OrderCancelContext(
                order.getId(),
                cancelProductAmount,
                cancelDeliveryFee,
                cancelProductAmount + cancelDeliveryFee,
                targets.stream()
                        .map(item -> new OrderCancelContext.CancelTargetItem(
                                item.getId(), item.getProductId(), item.getTimeDealId()))
                        .toList()
        );
    }

    @Transactional
    public OrderCancelResult applyCancel(
            CancelOrderCommand command,
            OrderCancelContext context,
            PaymentCancelReceipt receipt
    ) {
        // 락 획득 순서: 주문 -> 배송그룹 -> 아이템 -> 결제
        Order order = getOrder(context.orderId());
        Map<UUID, OrderDeliveryGroup> groups = groupsOf(order.getId());
        List<OrderItem> allItems = orderItemRepository.findByOrderId(order.getId());
        List<OrderItem> targets = targetsOf(allItems, command.orderItemIds());

        // PG 취소가 트랜잭션 밖에서 선행되는 사이 다른 요청이 같은 아이템을 취소할 경우 처리
        verifyCancelable(targets, groups);

        order.applyCancellation(context.cancelTotalAmount());

        Instant canceledAt = Instant.now();
        fullyCanceledGroupIds(allItems, targets)
                .forEach(groupId -> groups.get(groupId).cancel(canceledAt));

        OrderCancel orderCancel = orderCancelRepository.save(
                OrderCancel.create(
                        order.getId(),
                        command.cancelReasonCode(),
                        command.cancelReason(),
                        command.canceledByType(),
                        command.requesterId().toString(),
                        context.cancelProductAmount(),
                        context.cancelDeliveryFee(),
                        receipt != null,
                        command.idempotencyKey()
                )
        );

        targets.forEach(item -> item.cancel(orderCancel.getId()));

        // 결제 전 주문은 남은 아이템이 없으면 더 진행할 것이 없어 중단 처리
        if (order.getOrderStatus() != OrderStatus.PAID && isFullyCanceled(allItems)) {
            order.abort();
        }

        OrderCancelResult.PaymentSummary payment = null;
        if (receipt != null) {
            PaymentCancelView canceled = paymentCancelUseCase.applyCancellation(order.getId(), receipt);
            orderCancel.linkPaymentTransaction(canceled.paymentTransactionId());
            payment = new OrderCancelResult.PaymentSummary(
                    canceled.paymentId(), canceled.paymentStatus(), canceled.balanceAmount(), canceled.canceledAmount());
        }

        return OrderCancelResult.of(orderCancel, order.getCanceledAmount(), targets, payment);
    }

    private void verifyOwnership(
            Order order,
            List<OrderItem> targets,
            Map<UUID, OrderDeliveryGroup> groups,
            CancelOrderCommand command
    ) {
        boolean owned = command.canceledByType() == CanceledByType.SELLER
                ? targets.stream().allMatch(item ->
                        groups.get(item.getDeliveryGroupId()).getSellerId().equals(command.requesterId()))
                : order.getUserId().equals(command.requesterId());

        if (!owned) {
            throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
        }
    }

    private void verifyCancelable(List<OrderItem> targets, Map<UUID, OrderDeliveryGroup> groups) {
        for (OrderItem item : targets) {
            DeliveryGroupStatus groupStatus = groups.get(item.getDeliveryGroupId()).getGroupStatus();
            // 배송이 시작된 건은 취소가 아니라 환불로 안내해야 해서 상태 위반과 구분한다.
            if (groupStatus == DeliveryGroupStatus.SHIPPED || groupStatus == DeliveryGroupStatus.DELIVERED) {
                throw new BusinessException(ErrorCode.ORDER_ALREADY_SHIPPED);
            }
            if (!item.isCancelable(groupStatus)) {
                throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
            }
        }
    }

    // 이번 취소로 ORDERED 아이템이 하나도 남지 않는 배송그룹.
    // 배송비 전액 환불과 그룹 취소의 기준
    private Set<UUID> fullyCanceledGroupIds(List<OrderItem> allItems, List<OrderItem> targets) {
        Set<UUID> targetIds = targets.stream().map(OrderItem::getId).collect(Collectors.toSet());

        return targets.stream()
                .map(OrderItem::getDeliveryGroupId)
                .distinct()
                .filter(groupId -> allItems.stream()
                        .filter(item -> item.getDeliveryGroupId().equals(groupId))
                        .filter(item -> item.getItemStatus() == OrderItemStatus.ORDERED)
                        .allMatch(item -> targetIds.contains(item.getId())))
                .collect(Collectors.toSet());
    }

    private boolean isFullyCanceled(List<OrderItem> allItems) {
        return allItems.stream().allMatch(item ->
                item.getItemStatus() == OrderItemStatus.CANCELED || item.getItemStatus() == OrderItemStatus.REFUNDED);
    }

    private List<OrderItem> targetsOf(List<OrderItem> allItems, List<UUID> orderItemIds) {
        List<OrderItem> targets = allItems.stream()
                .filter(item -> orderItemIds.contains(item.getId()))
                .toList();

        if (targets.size() != orderItemIds.size()) {
            throw new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND);
        }

        return targets;
    }

    private Map<UUID, OrderDeliveryGroup> groupsOf(UUID orderId) {
        return orderDeliveryGroupRepository.findByOrderId(orderId).stream()
                .collect(Collectors.toMap(OrderDeliveryGroup::getId, Function.identity()));
    }

    private OrderCancelResult.PaymentSummary paymentSummary(UUID orderId) {
        return paymentQueryUseCase.getPayment(orderId)
                .map(payment -> new OrderCancelResult.PaymentSummary(
                        payment.paymentId(), payment.paymentStatus(), payment.balanceAmount(), payment.canceledAmount()))
                .orElse(null);
    }

    private Order getOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }
}
