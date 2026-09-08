package com.parut.product.timedeal.domain.timedeal;

import com.parut.product.global.common.entity.DeletableEntity;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_time_deals")
public class TimeDeal extends DeletableEntity {

    private static final BigDecimal MIN_DISCOUNT_RATE = BigDecimal.ZERO;
    private static final BigDecimal MAX_DISCOUNT_RATE = BigDecimal.valueOf(100);

    @Column(name = "product_id", columnDefinition = "uuid")
    private UUID productId;

    @Column(name = "original_price", nullable = false)
    private Long originalPrice;

    @Column(name = "deal_price", nullable = false)
    private Long dealPrice;

    @Column(name = "discount_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountRate;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "max_purchase_quantity", nullable = false)
    private Integer maxPurchaseQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TimeDealStatus status;

    private TimeDeal(
            UUID productId,
            Long originalPrice,
            BigDecimal discountRate,
            Instant startAt,
            Instant endAt,
            Integer maxPurchaseQuantity,
            Instant now
    ) {
        validateRequiredFields(originalPrice, discountRate, startAt, endAt, maxPurchaseQuantity);
        validatePeriod(startAt, endAt, now);
        validateMaxPurchaseQuantity(maxPurchaseQuantity);
        validateOriginalPrice(originalPrice);
        validateDiscountRate(discountRate);

        this.productId = productId;
        this.originalPrice = originalPrice;
        this.discountRate = discountRate;
        this.dealPrice = calculateTimeDealPrice(originalPrice, discountRate);
        this.startAt = startAt;
        this.endAt = endAt;
        this.maxPurchaseQuantity = maxPurchaseQuantity;
        this.status = TimeDealStatus.SCHEDULED;
    }

    // NOTE: 시간 판정에 쓰는 now는 항상 파라미터로 받는다 — 유즈케이스당 하나로 고정하고 테스트에서 조작하기 위함.
    public static TimeDeal create(
            UUID productId,
            Long originalPrice,
            BigDecimal discountRate,
            Instant startAt,
            Instant endAt,
            Integer maxPurchaseQuantity,
            Instant now
    ) {
        return new TimeDeal(productId, originalPrice, discountRate, startAt, endAt, maxPurchaseQuantity, now);
    }

    // NOTE: SCHEDULED에서만 수정 가능하다 — 현재 필드가 전부 핵심 판매 조건이라 ACTIVE에서 바꿀 것이 없다.
    // NOTE: PATCH 부분 수정(null = 변경 없음). 검증은 병합한 뒤의 값으로 해야 기간 역전 같은 조합 오류를 잡는다.
    public void update(
            Long originalPrice,
            BigDecimal discountRate,
            Instant startAt,
            Instant endAt,
            Integer maxPurchaseQuantity,
            Instant now
    ) {
        if (isDeleted()) {
            throw new BusinessException(ErrorCode.TIME_DEAL_DELETED);
        }
        if (status != TimeDealStatus.SCHEDULED) {
            throw new BusinessException(ErrorCode.TIME_DEAL_UPDATE_NOT_ALLOWED);
        }

        Long newOriginalPrice = originalPrice != null ? originalPrice : this.originalPrice;
        BigDecimal newDiscountRate = discountRate != null ? discountRate : this.discountRate;
        Instant newStartAt = startAt != null ? startAt : this.startAt;
        Instant newEndAt = endAt != null ? endAt : this.endAt;
        Integer newMaxPurchaseQuantity =
                maxPurchaseQuantity != null ? maxPurchaseQuantity : this.maxPurchaseQuantity;

        validatePeriod(newStartAt, newEndAt, now);
        validateMaxPurchaseQuantity(newMaxPurchaseQuantity);
        validateOriginalPrice(newOriginalPrice);
        validateDiscountRate(newDiscountRate);

        this.originalPrice = newOriginalPrice;
        this.discountRate = newDiscountRate;
        this.dealPrice = calculateTimeDealPrice(newOriginalPrice, newDiscountRate);
        this.startAt = newStartAt;
        this.endAt = newEndAt;
        this.maxPurchaseQuantity = newMaxPurchaseQuantity;
    }

    // NOTE: 판매 기간 안에서만 활성화한다. endAt이 지난 SCHEDULED 타임딜은 activate()가 아니라 end()로 정리한다.
    public void activate(Instant now) {
        if (now == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (isDeleted()) {
            throw new BusinessException(ErrorCode.TIME_DEAL_DELETED);
        }
        if (status != TimeDealStatus.SCHEDULED) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_STATUS_TRANSITION);
        }
        if (now.isBefore(startAt) || !now.isBefore(endAt)) {
            throw new BusinessException(ErrorCode.TIME_DEAL_SALE_PERIOD_INVALID);
        }
        this.status = TimeDealStatus.ACTIVE;
    }

    // NOTE: SCHEDULED에서도 종료를 허용한다 — 판매 기간이 배치 주기보다 짧으면 영구히 SCHEDULED로 남는다.
    // NOTE: 시간 가드가 없는 이유는 재고 소진 조기 종료 때문이며, 호출 경로 제한은 TimeDealPolicy 책임이다.
    public void end() {
        if (status != TimeDealStatus.SCHEDULED && status != TimeDealStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_STATUS_TRANSITION);
        }
        this.status = TimeDealStatus.ENDED;
    }

    public void stop() {
        if (status != TimeDealStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.TIME_DEAL_NOT_ACTIVE);
        }
        this.status = TimeDealStatus.STOPPED;
    }

    // NOTE: 검증만 분리해둔 이유는 TimeDealPolicy가 재고까지 함께 확인한 뒤에 변경을 시작하기 위함이다
    // (하나만 바뀐 채로 예외가 나가는 것을 막는다).
    public void validateDeletable() {
        if (status == TimeDealStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.TIME_DEAL_ACTIVE_DELETE_NOT_ALLOWED);
        }
        if (status != TimeDealStatus.SCHEDULED) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_STATUS);
        }
    }

    @Override
    public void softDelete(String deletedBy) {
        validateDeletable();
        super.softDelete(deletedBy);
    }

    public void validatePurchasable(Instant now) {
        if (now == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (isDeleted()) {
            throw new BusinessException(ErrorCode.TIME_DEAL_DELETED);
        }
        if (status == TimeDealStatus.STOPPED || status == TimeDealStatus.ENDED) {
            throw new BusinessException(ErrorCode.TIME_DEAL_NOT_ACTIVE);
        }
        // NOTE: 배치가 늦어도 정확하도록 구매 가능 여부는 status가 아니라 시간으로 판단한다.
        if (now.isBefore(startAt) || now.isAfter(endAt)) {
            throw new BusinessException(ErrorCode.TIME_DEAL_SALE_PERIOD_INVALID);
        }
    }

    // NOTE: maxPurchaseQuantity는 1인당 누적 수량이다 — 단건만 비교하면 주문을 나눠 우회할 수 있다.
    // 누적 수량은 저장소 조회가 필요해 Application Service가 넘기고, 합산·비교 규칙만 여기 둔다.
    public void validatePurchaseQuantity(Integer quantity, Integer alreadyPurchasedQuantity) {
        if (quantity == null || alreadyPurchasedQuantity == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        // NOTE: 막지 않으면 음수 수량이 합계를 줄여 상한 비교를 통과한다(누적 8 + 요청 -5 = 3 <= 10).
        if (quantity < 1) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_PURCHASE_QUANTITY);
        }
        // NOTE: 저장소에서 집계해 넘어오는 값이라 0 이상이어야 정상이다.
        if (alreadyPurchasedQuantity < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (alreadyPurchasedQuantity + quantity > this.maxPurchaseQuantity) {
            throw new BusinessException(ErrorCode.TIME_DEAL_EXCEEDS_MAX_PURCHASE_QUANTITY);
        }
    }

    private static void validateRequiredFields(
            Long originalPrice,
            BigDecimal discountRate,
            Instant startAt,
            Instant endAt,
            Integer maxPurchaseQuantity
    ) {
        if (originalPrice == null
                || discountRate == null
                || startAt == null
                || endAt == null
                || maxPurchaseQuantity == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private static void validatePeriod(Instant startAt, Instant endAt, Instant now) {
        if (startAt == null || endAt == null || now == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (!endAt.isAfter(startAt)) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_PERIOD);
        }
        // NOTE: 이미 끝난 타임딜은 만들 수 없다.
        if (!endAt.isAfter(now)) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_PERIOD);
        }
    }

    private static void validateMaxPurchaseQuantity(Integer maxPurchaseQuantity) {
        if (maxPurchaseQuantity == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (maxPurchaseQuantity <= 0) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_MAX_PURCHASE_QUANTITY);
        }
    }

    private static void validateOriginalPrice(Long originalPrice) {
        if (originalPrice == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (originalPrice <= 0) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_PRICE);
        }
    }

    private static void validateDiscountRate(BigDecimal discountRate) {
        if (discountRate == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (discountRate.compareTo(MIN_DISCOUNT_RATE) < 0 || discountRate.compareTo(MAX_DISCOUNT_RATE) >= 0) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_DISCOUNT_RATE);
        }
    }

    private static Long calculateTimeDealPrice(Long originalPrice, BigDecimal discountRate) {
        BigDecimal original = BigDecimal.valueOf(originalPrice);
        BigDecimal discountAmount = original.multiply(discountRate).divide(BigDecimal.valueOf(100));
        return original.subtract(discountAmount).setScale(0, RoundingMode.DOWN).longValueExact();
    }
}