package com.parut.product.timedeal.domain.timedealpurchase;

import com.parut.product.global.common.entity.DeletableEntity;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_time_deal_purchases")
public class TimeDealPurchase extends DeletableEntity {

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
            Instant reservedAt,
            Instant expiresAt
    ) {
        validateRequiredFields(timeDeal, orderId, userId, quantity, reservedAt, expiresAt);
        timeDeal.validatePurchasable();
        validateQuantity(timeDeal, quantity);
        validateReservationPeriod(reservedAt, expiresAt);

        this.orderId = orderId;
        this.timeDealId = timeDeal.getId();
        this.userId = userId;
        this.quantity = quantity;
        this.status = TimeDealPurchaseStatus.RESERVED;
        this.reservedAt = reservedAt;
        this.expiresAt = expiresAt;
    }

    public static TimeDealPurchase create(
            TimeDeal timeDeal,
            UUID orderId,
            UUID userId,
            Integer quantity,
            Instant reservedAt,
            Instant expiresAt
    ) {
        return new TimeDealPurchase(timeDeal, orderId, userId, quantity, reservedAt, expiresAt);
    }

    private static void validateRequiredFields(
            TimeDeal timeDeal,
            UUID orderId,
            UUID userId,
            Integer quantity,
            Instant reservedAt,
            Instant expiresAt
    ) {
        if (timeDeal == null
                || orderId == null
                || userId == null
                || quantity == null
                || reservedAt == null
                || expiresAt == null) {
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

    private static void validateReservationPeriod(Instant reservedAt, Instant expiresAt) {
        if (reservedAt == null || expiresAt == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (!expiresAt.isAfter(reservedAt)) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_RESERVATION_EXPIRATION);
        }
    }
}