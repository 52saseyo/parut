package com.parut.order.delivery.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parut.order.delivery.application.dto.DeliveryGroupSnapshot;
import com.parut.order.delivery.application.event.DeliveryStatusChangedEvent;
import com.parut.order.delivery.application.port.OrderDeliveryGroupQueryPort;
import com.parut.order.delivery.domain.Delivery;
import com.parut.order.delivery.domain.DeliveryStatus;
import com.parut.order.delivery.infrastructure.persistence.DeliveryRepository;
import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 배송 생성과 상태 변경을 처리한다.
 *
 * <p>상태 전이는 {@link Delivery}에 맡기고 주문 정보 조회, 권한 검증, 트랜잭션과 상태 변경 이벤트 발행을 조율한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryService {

    /** 배송 시작 6시간 후 자동완료한다. */
    private static final long DELIVERY_COMPLETION_DELAY_HOURS = 6L;

    private final DeliveryRepository deliveryRepository;

    // NOTE: Order 구현이 들어오기 전까지는 조회 Port 없이도 애플리케이션이 기동되어야 한다.
    private final ObjectProvider<OrderDeliveryGroupQueryPort> orderDeliveryGroupQueryPortProvider;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    private Delivery findOrCreateDelivery(UUID deliveryGroupId) {
        // TODO: UNIQUE 충돌 시 기존 배송을 다시 조회해 반환한다.
        return deliveryRepository.findByDeliveryGroupId(deliveryGroupId)
                .orElseGet(() -> deliveryRepository.save(Delivery.create(deliveryGroupId)));
    }

    @Transactional
    public List<Delivery> initializeDeliveriesForOrder(UUID orderId) {
        if (orderId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return requireOrderQueryPort()
                .findAllByOrderId(orderId)
                .stream()
                .map(DeliveryGroupSnapshot::deliveryGroupId)
                .distinct()
                .map(this::findOrCreateDelivery)
                .toList();
    }

    private Delivery getDelivery(UUID deliveryId) {
        return deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));
    }

    /**
     * 운송장을 등록하기 전에 판매자 소유권과 발송할 상품이 남아 있는지 확인한다.
     */
    @Transactional
    public Delivery startDelivery(
            UUID deliveryId,
            UUID sellerId,
            String trackingNumber
    ) {
        if (deliveryId == null || sellerId == null
                || trackingNumber == null || trackingNumber.isBlank() || trackingNumber.length() > 30) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Delivery delivery = getDelivery(deliveryId);
        if (delivery.getStatus() != DeliveryStatus.PREPARING) {
            throw new BusinessException(ErrorCode.DELIVERY_INVALID_STATUS_TRANSITION);
        }

        DeliveryGroupSnapshot deliveryGroup = requireOrderQueryPort()
                .findById(delivery.getDeliveryGroupId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));

        if (!sellerId.equals(deliveryGroup.sellerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (deliveryGroup.nonCanceledItemCount() <= 0) {
            throw new BusinessException(ErrorCode.DELIVERY_NO_SHIPPABLE_ITEMS);
        }

        OffsetDateTime shippedAt = OffsetDateTime.now();
        delivery.ship(trackingNumber, shippedAt);
        eventPublisher.publishEvent(DeliveryStatusChangedEvent.shipped(delivery.getDeliveryGroupId()));

        return delivery;
    }

    /**
     * 시작한 지 6시간이 지난 배송을 완료한다.
     */
    @Transactional
    public int completeEligibleDeliveries(OffsetDateTime completionTime) {
        if (completionTime == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        List<Delivery> deliveries = deliveryRepository.findAllByStatusAndShippedAtLessThanEqual(
                DeliveryStatus.SHIPPED,
                completionTime.minusHours(DELIVERY_COMPLETION_DELAY_HOURS)
        );

        deliveries.forEach(delivery -> {
            delivery.complete(completionTime);
            eventPublisher.publishEvent(DeliveryStatusChangedEvent.delivered(delivery.getDeliveryGroupId()));
        });
        return deliveries.size();
    }

    private OrderDeliveryGroupQueryPort requireOrderQueryPort() {
        OrderDeliveryGroupQueryPort port = orderDeliveryGroupQueryPortProvider.getIfAvailable();
        if (port == null) {
            throw new IllegalStateException("Order 배송 그룹 조회 Port 구현이 필요합니다.");
        }
        return port;
    }
}
