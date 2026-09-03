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

    // cancel_reason 컬럼 길이(V8__create_time_deal_purchase.sql)와 맞춰둔 값
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
        // 판매 가능 여부의 기준 시각은 선점 시각(reservedAt)과 같아야 한다 —
        // 여기서 별도로 now를 조달하면 검증 시각과 기록되는 선점 시각이 어긋날 수 있다.
        timeDeal.validatePurchasable(reservedAt);
        validateQuantity(quantity);

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
    // NOTE: 1인당 누적 구매 제한(TimeDeal.validatePurchaseQuantity)은 여기서 검증하지 않음 —
    // 같은 (timeDealId, userId)의 기존 purchase 집합을 조회해야 알 수 있는 규칙이라 엔티티가 스스로 지킬 수 없고,
    // 넘겨받은 누적 수량이 진짜인지도 도메인이 확인할 수 없다. orderId 중복 검증과 같은 범주로 Application Service 책임.
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
    // NOTE: status가 아직 RESERVED여도 만료 배치가 expire()를 못 돌렸을 뿐일 수 있으므로,
    // 확정 가능 여부는 status만 믿지 않고 만료 시각을 직접 확인한다.
    // (TimeDeal.validatePurchasable()이 오픈/마감을 시간으로 재확인하는 것과 같은 지연 평가 원리 —
    // 이 검증이 없으면 선점 10분이 지난 뒤 결제가 성공한 건도 판매 확정되어 정책이 뚫린다.)
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

    // NOTE: 배송 전 주문 취소 정책 — RESERVED/CONFIRMED 둘 다 취소 가능(배송 시작 전까지는 결제 완료 건도 취소 가능).
    // 배송 완료 후 "환불"은 별도 흐름이며 재고를 복구하지 않으므로 이 메서드를 쓰지 않는다.
    // NOTE: 취소 전 상태에 따라 Application Service가 재고 처리를 다르게 호출해야 함 —
    // RESERVED였다면 TimeDealStock.cancelReservation(), CONFIRMED였다면 TimeDealStock.cancelSale() (여기서 직접 호출하지 않음)
    // NOTE: reason은 null을 허용한다(사유 없는 취소 가능). 다만 cancel_reason 컬럼이 VARCHAR(30)이라
    // 길이를 여기서 막아준다 — 막지 않으면 30자 초과 문구가 DB에서 터져 500으로 나간다.
    public void cancel(String reason) {
        validateCancelReason(reason);
        if (status != TimeDealPurchaseStatus.RESERVED && status != TimeDealPurchaseStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_STATUS_TRANSITION);
        }
        this.status = TimeDealPurchaseStatus.CANCELLED;
        this.cancelReason = reason;
    }

    // NOTE: cancel()과 마찬가지로 재고 복구(TimeDealStock.cancelReservation())는 Application Service 책임
    // NOTE: cancelReason은 String 그대로 두어 어떤 문구든 담을 수 있게 하고,
    // 자주 쓰는 사유만 TimeDealPurchaseCancelReason에 모아 문구 중복·오타를 막는다.
    public void expire(Instant now) {
        if (!isExpired(now)) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_STATUS_TRANSITION);
        }
        cancel(TimeDealPurchaseCancelReason.RESERVATION_EXPIRED.name());
    }

    // NOTE: 구매 이력은 어떤 상태에서도 삭제할 수 없다 — 정산·환불·통계의 근거이므로
    // CANCELLED 건도 남겨야 한다(취소 후 재요청 가능 여부 판단에도 쓰인다).
    // 삭제 대신 상태 전이(cancel/expire)로만 생명주기를 표현한다.
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