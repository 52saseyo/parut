package com.parut.product.timedeal.domain.timedeal;

import com.parut.product.global.common.entity.DeletableEntity;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_time_deals")
public class TimeDeal extends DeletableEntity {

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
            Long dealPrice,
            BigDecimal discountRate,
            Instant startAt,
            Instant endAt,
            Integer maxPurchaseQuantity
    ) {
        validateRequiredFields(originalPrice, dealPrice, discountRate, startAt, endAt, maxPurchaseQuantity);
        validatePeriod(startAt, endAt);
        validateMaxPurchaseQuantity(maxPurchaseQuantity);
        validatePrice(dealPrice);

        this.productId = productId;
        this.originalPrice = originalPrice;
        this.dealPrice = dealPrice;
        this.discountRate = discountRate;
        this.startAt = startAt;
        this.endAt = endAt;
        this.maxPurchaseQuantity = maxPurchaseQuantity;
        this.status = TimeDealStatus.SCHEDULED;
    }

    public static TimeDeal create(
            UUID productId,
            Long originalPrice,
            Long dealPrice,
            BigDecimal discountRate,
            Instant startAt,
            Instant endAt,
            Integer maxPurchaseQuantity
    ) {
        return new TimeDeal(productId, originalPrice, dealPrice, discountRate, startAt, endAt, maxPurchaseQuantity);
    }

    public void activate() {
        if (status != TimeDealStatus.SCHEDULED) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_STATUS_TRANSITION);
        }
        this.status = TimeDealStatus.ACTIVE;
    }

    public void end() {
        if (status != TimeDealStatus.ACTIVE) {
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

    @Override
    public void softDelete(String deletedBy) {
        if (status == TimeDealStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.TIME_DEAL_ACTIVE_DELETE_NOT_ALLOWED);
        }
        if (status != TimeDealStatus.SCHEDULED) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_STATUS);
        }
        super.softDelete(deletedBy);
    }

    public void validatePurchasable() {
        if (status == TimeDealStatus.STOPPED || status == TimeDealStatus.ENDED) {
            throw new BusinessException(ErrorCode.TIME_DEAL_NOT_ACTIVE);
        }
        // status가 아직 SCHEDULED여도 배치가 activate()를 못 돌렸을 뿐일 수 있으므로,
        // 실제 구매 가능 여부는 배치 결과가 아니라 시간 자체로 판단한다.
        Instant now = Instant.now();
        if (now.isBefore(startAt) || now.isAfter(endAt)) {
            throw new BusinessException(ErrorCode.TIME_DEAL_SALE_PERIOD_INVALID);
        }
    }

    public void validatePurchaseQuantity(Integer quantity) {
        if (quantity > this.maxPurchaseQuantity) {
            throw new BusinessException(ErrorCode.TIME_DEAL_EXCEEDS_MAX_PURCHASE_QUANTITY);
        }
    }

    private static void validateRequiredFields(
            Long originalPrice,
            Long dealPrice,
            BigDecimal discountRate,
            Instant startAt,
            Instant endAt,
            Integer maxPurchaseQuantity
    ) {
        if (originalPrice == null
                || dealPrice == null
                || discountRate == null
                || startAt == null
                || endAt == null
                || maxPurchaseQuantity == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private static void validatePeriod(Instant startAt, Instant endAt) {
        if (startAt == null || endAt == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (!endAt.isAfter(startAt)) {
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

    private static void validatePrice(Long dealPrice) {
        if (dealPrice == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (dealPrice <= 0) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_PRICE);
        }
    }
}