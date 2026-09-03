package com.parut.product.product.domain.stock.entity;

import com.parut.product.global.common.entity.DeletableEntity;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.product.domain.stock.enums.StockStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "p_product_stocks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductStock extends DeletableEntity {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "low_stock_threshold", nullable = false)
    private int lowStockThreshold;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StockStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public static ProductStock create(UUID productId, int totalQuantity, int lowStockThreshold) {
        ProductStock stock = new ProductStock();
        stock.productId = productId;
        stock.totalQuantity = totalQuantity;
        stock.availableQuantity = totalQuantity;
        stock.lowStockThreshold = lowStockThreshold;
        stock.status = StockStatus.AVAILABLE;
        return stock;
    }

    public void reserve(int quantity) {
        validateOnSale();
        if (this.availableQuantity < quantity) {
            throw new BusinessException(ErrorCode.STOCK_SHORTAGE);
        }
        this.availableQuantity -= quantity;
        refreshStatus();
    }

    // 확정 시 available은 예약 단계에서 이미 차감되어 total만 차감
    public void confirm(int quantity) {
        this.totalQuantity -= quantity;
        refreshStatus();
    }

    public void restore(int quantity) {
        this.availableQuantity += quantity;
        refreshStatus();
    }

    public void adjustQuantity(int totalQuantity, int availableQuantity) {
        this.totalQuantity = totalQuantity;
        this.availableQuantity = availableQuantity;
        refreshStatus();
    }

    private void validateOnSale() {
        if (this.status == StockStatus.SOLD_OUT) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_ON_SALE);
        }
    }

    // total 0이하 → SOLD_OUT, available 임계치 이하 → LOW_STOCK
    private void refreshStatus() {
        if (this.totalQuantity <= 0) {
            this.status = StockStatus.SOLD_OUT;
        } else if (this.availableQuantity <= this.lowStockThreshold) {
            this.status = StockStatus.LOW_STOCK;
        } else {
            this.status = StockStatus.AVAILABLE;
        }
    }

}
