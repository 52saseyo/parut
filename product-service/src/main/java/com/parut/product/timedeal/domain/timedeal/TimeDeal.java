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
        if (!endAt.isAfter(startAt)) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_PERIOD);
        }
        if (maxPurchaseQuantity <= 0) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_MAX_PURCHASE_QUANTITY);
        }
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
}