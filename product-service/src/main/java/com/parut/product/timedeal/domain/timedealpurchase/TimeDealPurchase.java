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

    // NOTE: 타임딜 선점 유지 시간 10분(일반 상품 30분과 다름). expiresAt을 계산으로 만들어 호출자가
    // 다른 TTL을 넣을 수 없게 한다.
    private static final Duration RESERVATION_TTL = Duration.ofMinutes(10);

    // NOTE: cancel_reason 컬럼 길이(V8__create_time_deal_purchase.sql)와 맞춰둔 값
    private static final int MAX_CANCEL_REASON_LENGTH = 30;

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
        // NOTE: 판정 기준 시각은 선점 시각과 같아야 하므로 reservedAt을 그대로 넘긴다.
        timeDeal.validatePurchasable(reservedAt);
        validateQuantity(quantity);

        this.orderId = orderId;
        this.timeDealId = timeDeal.getId();
        this.userId = userId;
        this.quantity = quantity;
        this.status = TimeDealPurchaseStatus.RESERVED;
        this.reservedAt = reservedAt;
        // NOTE: 계산된 값이라 만료 시각 역전이 구조적으로 불가능해 별도 검증이 없다.
        this.expiresAt = reservedAt.plus(RESERVATION_TTL);
    }

    // NOTE: 재고 선점과 1인당 누적 제한 검증은 여기서 하지 않는다 — 조율은 TimeDealPolicy 책임이고,
    // 누적 제한은 저장소 조회가 필요한 집합 규칙이라 엔티티가 스스로 지킬 수 없다.
    public static TimeDealPurchase create(
            TimeDeal timeDeal,
            UUID orderId,
            UUID userId,
            Integer quantity,
            Instant reservedAt
    ) {
        return new TimeDealPurchase(timeDeal, orderId, userId, quantity, reservedAt);
    }

    // NOTE: status만 믿지 않고 만료 시각을 직접 확인한다 — 만료 배치가 늦으면 10분 지난 선점이
    // 그대로 확정되어 정책이 뚫린다.
    public void confirm(Instant now) {
        if (now == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (status != TimeDealPurchaseStatus.RESERVED) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_STATUS_TRANSITION);
        }
        if (isExpired(now)) {
            throw new BusinessException(ErrorCode.TIME_DEAL_RESERVATION_EXPIRED);
        }
        this.status = TimeDealPurchaseStatus.CONFIRMED;
    }

    // NOTE: 배송 시작 전이면 RESERVED/CONFIRMED 둘 다 취소 가능하다(배송 완료 후 환불은 별도 흐름).
    // reason은 null 허용이며 길이만 막는다 — 막지 않으면 VARCHAR(30) 초과가 DB에서 터져 500이 된다.
    public void cancel(String reason) {
        validateCancelReason(reason);
        if (status != TimeDealPurchaseStatus.RESERVED && status != TimeDealPurchaseStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_STATUS_TRANSITION);
        }
        this.status = TimeDealPurchaseStatus.CANCELLED;
        this.cancelReason = reason;
    }

    // NOTE: cancelReason은 String으로 두어 자유 문구를 허용하고, 자주 쓰는 사유만 enum 카탈로그로 모았다.
    public void expire(Instant now) {
        if (!isExpired(now)) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_STATUS_TRANSITION);
        }
        cancel(TimeDealPurchaseCancelReason.RESERVATION_EXPIRED.name());
    }

    // NOTE: 구매 이력은 어떤 상태에서도 삭제하지 않는다 — 정산·환불·통계의 근거이므로 CANCELLED도 남긴다.
    @Override
    public void softDelete(String deletedBy) {
        throw new BusinessException(ErrorCode.TIME_DEAL_PURCHASE_DELETE_NOT_ALLOWED);
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

    private static void validateCancelReason(String reason) {
        if (reason != null && reason.length() > MAX_CANCEL_REASON_LENGTH) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_CANCEL_REASON);
        }
    }

    private static void validateQuantity(Integer quantity) {
        if (quantity == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (quantity < 1) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_PURCHASE_QUANTITY);
        }
    }
}