package com.parut.product.product.domain.stock.entity;


import com.parut.product.global.common.entity.DeletableEntity;
import com.parut.product.product.domain.stock.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "p_product_stock_reservations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class ProductStockReservation extends DeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_stock_id", nullable = false)
    private ProductStock productStock;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public static ProductStockReservation create(ProductStock productStock, String orderId,
                                                 int quantity, LocalDateTime expiresAt) {
        ProductStockReservation reservation = new ProductStockReservation();
        reservation.productStock = productStock;
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
