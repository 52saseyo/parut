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
        this.dealPrice = calculateDealPrice(originalPrice, discountRate);
        this.startAt = startAt;
        this.endAt = endAt;
        this.maxPurchaseQuantity = maxPurchaseQuantity;
        this.status = TimeDealStatus.SCHEDULED;
    }

    // NOTE: 시간 판정에 쓰는 now는 도메인이 Instant.now()로 직접 조달하지 않고 항상 파라미터로 받는다.
    // 한 유즈케이스가 같은 now를 모든 도메인 호출에 넘기면 판정 기준 시각이 하나로 고정되고,
    // 테스트에서도 시각을 원하는 지점에 고정할 수 있다(종료 경계, 이미 끝난 딜 픽스처 등).
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

    // NOTE: 수정 정책 — SCHEDULED에서만 판매 조건을 수정할 수 있다.
    // ACTIVE는 dealPrice/startAt/endAt/maxPurchaseQuantity 같은 핵심 판매 조건을 바꿀 수 없고,
    // 현재 TimeDeal이 가진 필드가 전부 핵심 판매 조건이라 ACTIVE에서 수정 가능한 필드는 없다.
    // (설명·타이틀처럼 핵심 조건이 아닌 필드가 추가되면 그때 ACTIVE 허용 분기를 따로 만든다.)
    // ENDED/STOPPED는 수정 자체가 불가. productId는 생성 후 변경 불가라 파라미터에 없다.
    // NOTE: 재고 수량 변경은 여기서 하지 않음 — TimeDealStock 책임.
    // NOTE: PATCH 부분 수정 — null인 파라미터는 "변경하지 않음"을 뜻하므로 기존 값을 유지한다.
    // 검증은 병합한 뒤의 값으로 해야 한다. 예를 들어 startAt만 넘어왔다면 기존 endAt과 비교해야
    // 기간 역전을 잡을 수 있고, 개별 파라미터만 따로 검증하면 이 조합 오류를 놓친다.
    // dealPrice는 originalPrice와 discountRate 중 하나만 바뀌어도 어긋나므로 항상 다시 계산한다.
    public void update(
            Long originalPrice,
            BigDecimal discountRate,
            Instant startAt,
            Instant endAt,
            Integer maxPurchaseQuantity,
            Instant now
    ) {
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
        this.dealPrice = calculateDealPrice(newOriginalPrice, newDiscountRate);
        this.startAt = newStartAt;
        this.endAt = newEndAt;
        this.maxPurchaseQuantity = newMaxPurchaseQuantity;
    }

    public void activate() {
        if (status != TimeDealStatus.SCHEDULED) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_STATUS_TRANSITION);
        }
        this.status = TimeDealStatus.ACTIVE;
    }

    // NOTE: endAt 경과(배치) 또는 TimeDealStock.isDepleted() 감지(Application Service) 두 경로로 호출될 수 있음
    // NOTE: ACTIVE뿐 아니라 SCHEDULED에서도 종료를 허용한다 — 판매 기간이 배치 주기보다 짧으면
    // activate()가 한 번도 돌지 못한 채 endAt이 지날 수 있고, ACTIVE만 허용하면 그런 딜은 종료 배치가
    // 영구히 정리할 수 없어 SCHEDULED로 박혀 남는다(구매 자체는 validatePurchasable()이 시간으로 막지만
    // 예정 목록 노출·종료 이벤트 발행이 어긋난다). 판매가 열린 적이 없어도 시간상 끝난 것은 사실이므로 ENDED로 정리한다.
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

    public void validatePurchasable(Instant now) {
        if (now == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (status == TimeDealStatus.STOPPED || status == TimeDealStatus.ENDED) {
            throw new BusinessException(ErrorCode.TIME_DEAL_NOT_ACTIVE);
        }
        // status가 아직 SCHEDULED여도 배치가 activate()를 못 돌렸을 뿐일 수 있으므로,
        // 실제 구매 가능 여부는 배치 결과가 아니라 시간 자체로 판단한다.
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
        // 등록·수정 시점에 이미 끝난 타임딜은 만들 수 없다. 기준 시각을 주입받으므로
        // 테스트에서 now를 과거로 주면 "이미 종료된 딜" 픽스처도 만들 수 있다.
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

    private static Long calculateDealPrice(Long originalPrice, BigDecimal discountRate) {
        BigDecimal original = BigDecimal.valueOf(originalPrice);
        BigDecimal discountAmount = original.multiply(discountRate).divide(BigDecimal.valueOf(100));
        return original.subtract(discountAmount).setScale(0, RoundingMode.DOWN).longValueExact();
    }
}