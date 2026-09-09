package com.parut.order.order.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.parut.order.order.domain.CancelReasonCode;
import com.parut.order.order.domain.CanceledByType;
import com.parut.order.order.domain.DeliveryGroupStatus;
import com.parut.order.order.domain.Order;
import com.parut.order.order.domain.OrderCancel;
import com.parut.order.order.domain.OrderDeliveryGroup;
import com.parut.order.order.domain.OrderItem;
import com.parut.order.order.domain.OrderItemStatus;
import com.parut.order.order.domain.OrderStatus;
import com.parut.order.order.domain.OrderType;
import com.parut.order.payment.application.port.in.dto.PaymentView;
import com.parut.order.payment.domain.PaymentMethod;
import com.parut.order.payment.domain.PaymentStatus;

// 주문 상세 조회 결과. 엔티티를 그대로 담지 않고 표현에 필요한 값만 담는다 —
// presentation 계층이 이 값을 통해 order.cancel() 같은 도메인 메서드를 호출할 수 없게 하기 위함.
// cancelable/refundable도 from()에서 도메인 메서드를 호출해 이미 계산해 담는다.
public record OrderDetailData(
        UUID orderId,
        String orderNo,
        OrderType orderType,
        OrderStatus orderStatus,
        Long totalProductAmount,
        Long totalDeliveryFee,
        Long totalPaymentAmount,
        Long canceledAmount,
        Instant expiresAt,
        Instant orderedAt,
        Instant paidAt,
        Recipient recipient,
        PaymentSummary payment,
        List<DeliveryGroup> deliveryGroups,
        List<Cancel> cancels
) {
    public static OrderDetailData from(
            Order order,
            List<OrderDeliveryGroup> groups,
            List<OrderItem> items,
            PaymentView payment,
            List<OrderCancel> cancels
    ) {
        List<DeliveryGroup> deliveryGroups = groups.stream()
                .map(group -> DeliveryGroup.from(
                        group,
                        items.stream()
                                .filter(item -> item.getDeliveryGroupId().equals(group.getId()))
                                .toList()
                ))
                .toList();

        return new OrderDetailData(
                order.getId(),
                order.getOrderNo(),
                order.getOrderType(),
                order.getOrderStatus(),
                order.getTotalProductAmount(),
                order.getTotalDeliveryFee(),
                order.getTotalPaymentAmount(),
                order.getCanceledAmount(),
                order.getExpiresAt(),
                order.getOrderedAt(),
                order.getPaidAt(),
                Recipient.from(order),
                payment == null ? null : PaymentSummary.from(payment),
                deliveryGroups,
                cancels.stream().map(Cancel::from).toList()
        );
    }

    public record Recipient(
            String recipientName,
            String recipientPhone,
            String zipCode,
            String addressBase,
            String addressDetail,
            String deliveryRequest
    ) {
        static Recipient from(Order order) {
            return new Recipient(
                    order.getRecipientName(),
                    order.getRecipientPhone(),
                    order.getZipCode(),
                    order.getAddressBase(),
                    order.getAddressDetail(),
                    order.getDeliveryRequest()
            );
        }
    }

    public record PaymentSummary(
            UUID paymentId,
            PaymentStatus paymentStatus,
            PaymentMethod paymentMethod,
            Long totalAmount,
            Long canceledAmount,
            Instant approvedAt,
            String receiptUrl
    ) {
        static PaymentSummary from(PaymentView payment) {
            return new PaymentSummary(
                    payment.paymentId(),
                    payment.paymentStatus(),
                    payment.paymentMethod(),
                    payment.totalAmount(),
                    payment.canceledAmount(),
                    payment.approvedAt(),
                    payment.receiptUrl()
            );
        }
    }

    public record DeliveryGroup(
            UUID deliveryGroupId,
            UUID sellerId,
            DeliveryGroupStatus groupStatus,
            Long productAmount,
            Long deliveryFee,
            Instant canceledAt,
            List<Item> items
    ) {
        static DeliveryGroup from(OrderDeliveryGroup group, List<OrderItem> items) {
            return new DeliveryGroup(
                    group.getId(),
                    group.getSellerId(),
                    group.getGroupStatus(),
                    group.getProductAmount(),
                    group.getDeliveryFee(),
                    group.getCanceledAt(),
                    items.stream().map(item -> Item.from(item, group.getGroupStatus())).toList()
            );
        }
    }

    public record Item(
            UUID orderItemId,
            UUID productId,
            UUID timeDealId,
            String productName,
            String appearanceType,
            String origin,
            LocalDate harvestDate,
            String saleUnit,
            BigDecimal unitQuantity,
            Long originalPrice,
            Long unitPrice,
            Integer quantity,
            OrderItemStatus itemStatus,
            boolean cancelable,
            boolean refundable,
            Instant confirmedAt
    ) {
        static Item from(OrderItem item, DeliveryGroupStatus groupStatus) {
            return new Item(
                    item.getId(),
                    item.getProductId(),
                    item.getTimeDealId(),
                    item.getProductName(),
                    item.getAppearanceType(),
                    item.getOrigin(),
                    item.getHarvestDate(),
                    item.getSaleUnit(),
                    item.getUnitQuantity(),
                    item.getOriginalPrice(),
                    item.getUnitPrice(),
                    item.getQuantity(),
                    item.getItemStatus(),
                    item.isCancelable(groupStatus),
                    item.isRefundable(groupStatus),
                    item.getConfirmedAt()
            );
        }
    }

    public record Cancel(
            UUID cancelId,
            CancelReasonCode cancelReasonCode,
            String cancelReason,
            CanceledByType canceledByType,
            Long cancelProductAmount,
            Long cancelDeliveryFee,
            Long cancelTotalAmount,
            Boolean refundRequired,
            Instant canceledAt
    ) {
        static Cancel from(OrderCancel cancel) {
            return new Cancel(
                    cancel.getId(),
                    cancel.getCancelReasonCode(),
                    cancel.getCancelReason(),
                    cancel.getCanceledByType(),
                    cancel.getCancelProductAmount(),
                    cancel.getCancelDeliveryFee(),
                    cancel.getCancelTotalAmount(),
                    cancel.getRefundRequired(),
                    cancel.getCanceledAt()
            );
        }
    }
}
