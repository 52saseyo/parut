package com.parut.order.delivery.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parut.order.delivery.application.port.in.DeliveryCreateUseCase;
import com.parut.order.delivery.domain.Delivery;
import com.parut.order.delivery.domain.DeliveryStatus;
import com.parut.order.delivery.infrastructure.persistence.DeliveryRepository;
import com.parut.order.global.auth.UserRole;
import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.port.in.OrderDeliveryGroupQueryUseCase;
import com.parut.order.order.application.port.in.OrderDeliveryGroupStatusUseCase;
import com.parut.order.order.application.port.in.dto.OrderDeliveryGroupView;

import lombok.RequiredArgsConstructor;

/**
 * 배송 생성, 조회와 상태 변경을 처리한다.
 *
 * <p>상태 전이는 {@link Delivery}에 맡기고 주문 정보 조회, 소유권 검증, 트랜잭션과
 * Order 배송 그룹 상태 동기화를 조율한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryService implements DeliveryCreateUseCase {

    private final DeliveryRepository deliveryRepository;
    private final OrderDeliveryGroupQueryUseCase orderDeliveryGroupQueryUseCase;
    private final OrderDeliveryGroupStatusUseCase orderDeliveryGroupStatusUseCase;

    /**
     * 주문의 배송 그룹별 Delivery를 생성한다.
     *
     * <p>이미 생성된 배송은 유지하여 순차 재호출로 인한 중복 생성을 방지한다.
     */
    @Override
    @Transactional
    public void createDeliveries(UUID orderId) {
        if (orderId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        orderDeliveryGroupQueryUseCase.getDeliveryGroups(orderId).stream()
                .map(OrderDeliveryGroupView::deliveryGroupId)
                .forEach(this::findOrCreateDelivery);
    }

    public List<Delivery> getDeliveries(UUID orderId, UUID sellerId) {
        if (orderId == null || sellerId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return orderDeliveryGroupQueryUseCase.getDeliveryGroups(orderId).stream()
                .filter(deliveryGroup -> sellerId.equals(deliveryGroup.sellerId()))
                .map(OrderDeliveryGroupView::deliveryGroupId)
                .map(deliveryRepository::findByDeliveryGroupId)
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * 배송을 조회하고 역할에 따라 데이터 소유권을 확인한다.
     *
     * <p>역할 자체는 {@code UserContextInterceptor}가 검사하고, 여기서는 고객과
     * 판매자의 소유권 및 관리자의 전체 조회 범위만 판단한다.
     */
    public Delivery getDelivery(UUID deliveryId, UUID userId, UserRole userRole) {
        if (deliveryId == null || userId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));

        if (userRole == UserRole.ADMIN) {
            return delivery;
        }

        if (userRole == UserRole.CUSTOMER) {
            if (!orderDeliveryGroupQueryUseCase.isOwnedByCustomer(delivery.getDeliveryGroupId(), userId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        } else if (userRole == UserRole.SELLER) {
            OrderDeliveryGroupView group = orderDeliveryGroupQueryUseCase
                    .getDeliveryGroup(delivery.getDeliveryGroupId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
            if (!userId.equals(group.sellerId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        }
        return delivery;
    }

    // TODO: 결제 승인 흐름의 재시도 정책 확정 후 동시 생성 충돌 처리를 보강한다.
    private Delivery findOrCreateDelivery(UUID deliveryGroupId) {
        return deliveryRepository.findByDeliveryGroupId(deliveryGroupId)
                .orElseGet(() -> deliveryRepository.save(Delivery.create(deliveryGroupId)));
    }

    /**
     * 운송장을 등록하기 전에 판매자 소유권과 발송할 상품이 남아 있는지 확인한다.
     *
     * <p>배송 시작과 Order 배송 그룹 상태 전이(SHIPPED)를 같은 트랜잭션으로 묶어,
     * 한쪽만 반영되는 상태 불일치를 막는다.
     */
    @Transactional
    public Delivery startDelivery(UUID deliveryId, UUID sellerId, String trackingNumber) {
        if (deliveryId == null || sellerId == null
                || trackingNumber == null || trackingNumber.isBlank() || trackingNumber.length() > 30) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));
        if (delivery.getStatus() != DeliveryStatus.PREPARING) {
            throw new BusinessException(ErrorCode.DELIVERY_INVALID_STATUS_TRANSITION);
        }

        OrderDeliveryGroupView deliveryGroup = orderDeliveryGroupQueryUseCase
                .getDeliveryGroup(delivery.getDeliveryGroupId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOT_FOUND));

        if (!sellerId.equals(deliveryGroup.sellerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (deliveryGroup.shippableItemCount() <= 0) {
            throw new BusinessException(ErrorCode.DELIVERY_NO_SHIPPABLE_ITEMS);
        }

        Instant shippedAt = Instant.now();
        delivery.ship(trackingNumber, shippedAt);
        orderDeliveryGroupStatusUseCase.markShipped(delivery.getDeliveryGroupId());

        return delivery;
    }

    /**
     * 배송 한 건과 주문 배송 그룹을 같은 트랜잭션에서 완료한다.
     * 스케줄러가 이 메서드를 건별로 호출해 한 건의 실패가 다른 건에 영향을 주지 않는다.
     */
    @Transactional
    public void completeEligibleDelivery(UUID deliveryId, Instant completionTime, Instant completionThreshold) {
        if (deliveryId == null || completionTime == null || completionThreshold == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Delivery delivery = deliveryRepository.findById(deliveryId).orElse(null);
        if (delivery == null || delivery.getStatus() != DeliveryStatus.SHIPPED
                || delivery.getShippedAt().isAfter(completionThreshold)) {
            return;
        }

        delivery.complete(completionTime);
        orderDeliveryGroupStatusUseCase.markDelivered(delivery.getDeliveryGroupId());
    }
}
