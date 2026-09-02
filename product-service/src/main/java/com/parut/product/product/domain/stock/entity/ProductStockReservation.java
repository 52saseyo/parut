package com.parut.product.product.domain.stock.entity;


import com.parut.product.global.common.entity.DeletableEntity;
import com.parut.product.product.domain.stock.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "p_product_stock_reservations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class ProductStockReservation extends DeletableEntity {

    @Column(name = "stock_id", nullable = false)
    private UUID stockId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public static ProductStockReservation create(UUID stockId, UUID orderId,
                                                 int quantity, LocalDateTime expiresAt) {
        ProductStockReservation reservation = new ProductStockReservation();
        reservation.stockId = stockId;
        reservation.orderId = orderId;
        reservation.quantity = quantity;
        reservation.status = ReservationStatus.RESERVED;
        reservation.expiresAt = expiresAt;
        return reservation;
    }


    public void confirm() {
        validateReserved();
        this.status = ReservationStatus.CONFIRMED;
    }

    public void cancel() {
        validateReserved();
        this.status = ReservationStatus.CANCELLED;
    }

    private void validateReserved() {
        if(this.status != ReservationStatus.RESERVED) {
            throw new IllegalStateException("이미 처리된 예약입니다.");
        }
    }

}
