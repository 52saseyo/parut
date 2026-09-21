package com.parut.order.delivery.application;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.parut.order.delivery.application.port.in.DeliveryCompletionQueryUseCase;
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
public class DeliveryService implements DeliveryCreateUseCase, DeliveryCompletionQueryUseCase {

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
                .forEach(this::findOrCreateDelivery);
    }

    @Override
    public Optional<Instant> getDeliveredAt(UUID deliveryGroupId) {
        if (deliveryGroupId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return deliveryRepository.findByDeliveryGroupId(deliveryGroupId)
                .filter(delivery -> delivery.getStatus() == DeliveryStatus.DELIVERED)
                .map(Delivery::getDeliveredAt);
    }

    /** 고객과 판매자의 소유자 스냅샷을 기준으로 배송 목록을 커서 조회한다. */
    public DeliveryPage getDeliveries(
            UUID requesterId,
            UserRole requesterRole,
            UUID orderId,
            DeliveryStatus status,
            String cursor,
            UUID cursorId,
            int size
    ) {
        validateQuery(requesterId, requesterRole, cursor, cursorId, size);
        Instant cursorTime = parseCursor(cursor);
        PageRequest pageable = PageRequest.of(0, size + 1);

        List<Delivery> deliveries = switch (requesterRole) {
            case CUSTOMER -> deliveryRepository.findCustomerDeliveries(
                    requesterId, orderId, status, cursorTime, cursorId, pageable);
            case SELLER -> deliveryRepository.findSellerDeliveries(
                    requesterId, orderId, status, cursorTime, cursorId, pageable);
            default -> throw new BusinessException(ErrorCode.FORBIDDEN);
        };

        boolean hasNext = deliveries.size() > size;
        List<Delivery> content = hasNext ? deliveries.subList(0, size) : deliveries;
        if (!hasNext) {
            return new DeliveryPage(content, null, null, false);
        }

        Delivery lastDelivery = content.getLast();
        return new DeliveryPage(
                content,
                lastDelivery.getCreatedAt().toString(),
                lastDelivery.getId(),
                true
        );
    }

    /** 관리자가 소유자, 주문과 상태 조건으로 전체 배송을 조회한다. */
    public Page<Delivery> getAdminDeliveries(
            UUID customerId,
            UUID sellerId,
            UUID orderId,
            DeliveryStatus status,
            Pageable pageable
    ) {
        return deliveryRepository.findAdminDeliveries(customerId, sellerId, orderId, status, pageable);
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

        boolean accessible = switch (userRole) {
            case CUSTOMER -> userId.equals(delivery.getCustomerId());
            case SELLER -> userId.equals(delivery.getSellerId());
            case ADMIN -> true;
            default -> false;
        };
        if (!accessible) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return delivery;
    }

    private Delivery findOrCreateDelivery(OrderDeliveryGroupView group) {
        // NOTE: 동시 최초 생성은 delivery_group_id UNIQUE 제약으로 중복을 차단한다.
        // 실제 경합이 확인되면 충돌 후 재조회 또는 잠금 정책을 검토한다.
        return deliveryRepository.findByDeliveryGroupId(group.deliveryGroupId())
                .orElseGet(() -> deliveryRepository.save(Delivery.create(
                        group.deliveryGroupId(),
                        group.orderId(),
                        group.customerId(),
                        group.sellerId()
                )));
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
     * 호출자의 트랜잭션 유무와 관계없이 건별 새 트랜잭션을 사용해 다른 배송과 실패를 격리한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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

    private void validateQuery(
            UUID requesterId,
            UserRole requesterRole,
            String cursor,
            UUID cursorId,
            int size
    ) {
        boolean hasOnlyOneCursorValue = (cursor == null) != (cursorId == null);
        if (requesterId == null || requesterRole == null || hasOnlyOneCursorValue) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (size != 10 && size != 30 && size != 50) {
            throw new BusinessException(ErrorCode.INVALID_PAGE_SIZE);
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
