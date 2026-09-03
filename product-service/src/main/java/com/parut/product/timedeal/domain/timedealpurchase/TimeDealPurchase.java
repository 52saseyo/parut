package com.parut.product.timedeal.domain.timedealpurchase;

import com.parut.product.global.common.entity.DeletableEntity;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_time_deal_purchases")
public class TimeDealPurchase extends DeletableEntity {

    // 타임딜 선점 유지 시간 10분(일반 상품 30분과 다름). expiresAt을 파라미터로 받지 않고
    // 이 상수로 계산해, 호출자가 다른 TTL을 넣어 정책을 어기는 상태 자체를 만들 수 없게 한다.
    private static final Duration RESERVATION_TTL = Duration.ofMinutes(10);

    @Column(name = "order_id", columnDefinition = "uuid", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "time_deal_id", columnDefinition = "uuid", nullable = false)
    private UUID timeDealId;

    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TimeDealPurchaseStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "reserved_at", nullable = false)
    private Instant reservedAt;

    @Column(name = "cancel_reason", length = 30)
    private String cancelReason;

    private TimeDealPurchase(
            TimeDeal timeDeal,
            UUID orderId,
            UUID userId,
            Integer quantity,
            Instant reservedAt
    ) {
        validateRequiredFields(timeDeal, orderId, userId, quantity, reservedAt);
        // 판매 가능 여부의 기준 시각은 선점 시각(reservedAt)과 같아야 한다 —
        // 여기서 별도로 now를 조달하면 검증 시각과 기록되는 선점 시각이 어긋날 수 있다.
        timeDeal.validatePurchasable(reservedAt);
        validateQuantity(timeDeal, quantity);

        this.orderId = orderId;
        this.timeDealId = timeDeal.getId();
        this.userId = userId;
        this.quantity = quantity;
        this.status = TimeDealPurchaseStatus.RESERVED;
        this.reservedAt = reservedAt;
        // expiresAt은 입력값이 아니라 reservedAt + RESERVATION_TTL로 도메인이 계산한다.
        // 계산된 값이라 reservedAt보다 앞서는 만료 시각이 만들어질 수 없어 별도 검증이 필요 없다.
        this.expiresAt = reservedAt.plus(RESERVATION_TTL);
    }

    // NOTE: 재고 선점(TimeDealStock.reserve())은 여기서 하지 않음 — Application Service가 같은 트랜잭션에서 별도 호출해야 함
    public static TimeDealPurchase create(
            TimeDeal timeDeal,
            UUID orderId,
            UUID userId,
            Integer quantity,
            Instant reservedAt
    ) {
        return new TimeDealPurchase(timeDeal, orderId, userId, quantity, reservedAt);
    }

    // NOTE: 재고 판매 확정(TimeDealStock.confirmSale())은 여기서 하지 않음 — Application Service가 같은 트랜잭션에서 별도 호출해야 함
    public void confirm() {
        if (status != TimeDealPurchaseStatus.RESERVED) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_STATUS_TRANSITION);
        }
        this.status = TimeDealPurchaseStatus.CONFIRMED;
    }

    // NOTE: 배송 전 주문 취소 정책 — RESERVED/CONFIRMED 둘 다 취소 가능(배송 시작 전까지는 결제 완료 건도 취소 가능).
    // 배송 완료 후 "환불"은 별도 흐름이며 재고를 복구하지 않으므로 이 메서드를 쓰지 않는다.
    // NOTE: 취소 전 상태에 따라 Application Service가 재고 처리를 다르게 호출해야 함 —
    // RESERVED였다면 TimeDealStock.cancelReservation(), CONFIRMED였다면 TimeDealStock.cancelSale() (여기서 직접 호출하지 않음)
    public void cancel(String reason) {
        if (status != TimeDealPurchaseStatus.RESERVED && status != TimeDealPurchaseStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_STATUS_TRANSITION);
        }
        this.status = TimeDealPurchaseStatus.CANCELLED;
        this.cancelReason = reason;
    }

    // NOTE: cancel()과 마찬가지로 재고 복구(TimeDealStock.cancelReservation())는 Application Service 책임
    public void expire(Instant now) {
        if (!isExpired(now)) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_STATUS_TRANSITION);
        }
        cancel("선점 시간 만료로 자동 취소");
    }

    public boolean isExpired(Instant now) {
        if (now == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return status == TimeDealPurchaseStatus.RESERVED && now.isAfter(expiresAt);
    }

    private static void validateRequiredFields(
            TimeDeal timeDeal,
            UUID orderId,
            UUID userId,
            Integer quantity,
            Instant reservedAt
    ) {
        if (timeDeal == null
                || orderId == null
                || userId == null
                || quantity == null
                || reservedAt == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private static void validateQuantity(TimeDeal timeDeal, Integer quantity) {
        if (timeDeal == null || quantity == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (quantity < 1) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_PURCHASE_QUANTITY);
        }
        timeDeal.validatePurchaseQuantity(quantity);
    }
}