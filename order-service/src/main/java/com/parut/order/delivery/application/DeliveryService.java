package com.parut.order.delivery.application;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parut.order.delivery.domain.Delivery;
import com.parut.order.delivery.domain.DeliveryStatus;
import com.parut.order.delivery.infrastructure.persistence.DeliveryRepository;
import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.port.in.OrderDeliveryGroupQueryUseCase;
import com.parut.order.order.application.port.in.OrderDeliveryGroupStatusUseCase;
import com.parut.order.order.application.port.in.dto.OrderDeliveryGroupView;

import lombok.RequiredArgsConstructor;

/**
 * 배송 생성과 상태 변경을 처리한다.
 *
 * <p>상태 전이는 {@link Delivery}에 맡기고 주문 정보 조회, 권한 검증, 트랜잭션과
 * Order 배송 그룹 상태 동기화를 조율한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryService {

    /** 시연을 위해 배송 시작 60초 후 자동완료한다. */
    private static final long DELIVERY_COMPLETION_DELAY_SECONDS = 60L;

    private final DeliveryRepository deliveryRepository;
    private final OrderDeliveryGroupQueryUseCase orderDeliveryGroupQueryUseCase;
    private final OrderDeliveryGroupStatusUseCase orderDeliveryGroupStatusUseCase;

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

        return orderDeliveryGroupQueryUseCase.getDeliveryGroups(orderId).stream()
                .map(OrderDeliveryGroupView::deliveryGroupId)
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
     *
     * <p>배송 시작과 Order 배송 그룹 상태 전이(SHIPPED)를 같은 트랜잭션으로 묶어,
     * 한쪽만 반영되는 상태 불일치를 막는다.
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

        OrderDeliveryGroupView deliveryGroup = orderDeliveryGroupQueryUseCase
                .getDeliveryGroup(delivery.getDeliveryGroupId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));

        if (!sellerId.equals(deliveryGroup.sellerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (deliveryGroup.nonCanceledItemCount() <= 0) {
            throw new BusinessException(ErrorCode.DELIVERY_NO_SHIPPABLE_ITEMS);
        }

        Instant shippedAt = Instant.now();
        delivery.ship(trackingNumber, shippedAt);
        orderDeliveryGroupStatusUseCase.markShipped(delivery.getDeliveryGroupId());

        return delivery;
    }

    /**
     * 시작한 지 60초가 지난 배송을 완료한다.
     */
    @Transactional
    public int completeEligibleDeliveries(Instant completionTime) {
        if (completionTime == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        List<Delivery> deliveries = deliveryRepository.findAllByStatusAndShippedAtLessThanEqual(
                DeliveryStatus.SHIPPED,
                completionTime.minus(Duration.ofSeconds(DELIVERY_COMPLETION_DELAY_SECONDS))
        );

        deliveries.forEach(delivery -> {
            delivery.complete(completionTime);
            orderDeliveryGroupStatusUseCase.markDelivered(delivery.getDeliveryGroupId());
        });
        return deliveries.size();
    }
}
