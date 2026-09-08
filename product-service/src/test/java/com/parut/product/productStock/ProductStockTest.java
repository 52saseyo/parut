package com.parut.product.productStock;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.product.domain.stock.entity.ProductStock;
import com.parut.product.product.domain.stock.enums.StockStatus;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
public class ProductStockTest {

    private ProductStock createStock(int total, int lowStockThreshold) {
        return ProductStock.create(UUID.randomUUID(), total, lowStockThreshold);
    }

    @Nested
    @DisplayName("create()")
    class Create {
        @Test
        @DisplayName("총 재고와 가용 재고가 동일하게 초기화되고 있는 상태는 AVAILABLE")
        void create_initializesCorrectly() {
            ProductStock stock = createStock(100, 10);

            log.info("[ProductStock.create] total={}, available={}, status={}",
                    stock.getTotalQuantity(), stock.getAvailableQuantity(), stock.getStatus());

            assertThat(stock.getTotalQuantity()).isEqualTo(100);
            assertThat(stock.getAvailableQuantity()).isEqualTo(100);
            assertThat(stock.getStatus()).isEqualTo(StockStatus.AVAILABLE);
        }
    }


    @Nested
    @DisplayName("reserve()")
    class Reserve {

        @Test
        @DisplayName("가용 재고가 충분하면 availableQuantity만 차감")
        void reserve_success_decreasesAvailableQuantity() {
            ProductStock stock = createStock(100, 10);
            stock.reserve(30);

            log.info("[ProductStock.reserve] 30개 예약 후 available={}, total={}",
                    stock.getAvailableQuantity(), stock.getTotalQuantity());

            assertThat(stock.getAvailableQuantity()).isEqualTo(70);
            assertThat(stock.getTotalQuantity()).isEqualTo(100);
        }

        @Test
        @DisplayName("가용 재고보다 많은 수량을 요청하면 예외가 발생")
        void reserve_shortage_throwsException() {
            ProductStock stock = createStock(10, 5);

            log.info("[ProductStock.reserve] 가용 재고(10)보다 많은 20개 예약 시도 -> 재고 부족 예외 기대");

            assertThatThrownBy(() -> stock.reserve(20))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_SHORTAGE);
        }

        @Test
        @DisplayName("품절 상태에서는 예약할 수 없음")
        void reserve_whenSoldOut_throwsException() {
            ProductStock stock = createStock(5, 1);
            stock.confirm(5);

            log.info("[ProductStock.reserve] 전량 확정(품절) 상태에서 1개 예약 시도 -> 판매중지 예외 기대, status={}",
                    stock.getStatus());

            assertThatThrownBy(()->stock.reserve(1))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_PRODUCT_NOT_ON_SALE);
        }

        @Test
        @DisplayName("가용 재고가 임계치 이하로 내려가면 LOW_STOCK으로 전이")
        void reserve_belowThreshold_transitionToLowStock() {
            ProductStock stock = createStock(100, 20);
            stock.reserve(85); // available = 15 <= threshold = 20

            log.info("[ProductStock.reserve] 85개 예약 후 available={} (임계치 20) -> status={}",
                    stock.getAvailableQuantity(), stock.getStatus());

            assertThat(stock.getStatus()).isEqualTo(StockStatus.LOW_STOCK);
        }
    }

    @Nested
    @DisplayName("confirm()")
    class Confirm {

        @Test
        @DisplayName("확정 시 totalQuantity만 차감되고 avilableQuantity는 그대로")
        void confirm_decreasesTotalOnly() {
            ProductStock stock = createStock(100, 10);
            stock.reserve(30);

            stock.confirm(30);

            log.info("[ProductStock.confirm] 30개 확정 후 total={}, available={}",
                    stock.getTotalQuantity(), stock.getAvailableQuantity());

            assertThat(stock.getTotalQuantity()).isEqualTo(70);
            assertThat(stock.getAvailableQuantity()).isEqualTo(70);
        }

        @Test
        @DisplayName("totalQuantity가 0이 되면 SOLD_OUT으로 전이")
        void confirm_toZero_transitionsToSoldOut() {
            ProductStock stock = createStock(10, 2);
            stock.reserve(10);

            stock.confirm(10);

            log.info("[ProductStock.confirm] 전량(10개) 확정 후 total={} -> status={}",
                    stock.getTotalQuantity(), stock.getStatus());

            assertThat(stock.getStatus()).isEqualTo(StockStatus.SOLD_OUT);
        }
    }
    @Nested
    @DisplayName("restore()")
    class Restore {

        @Test
        @DisplayName("복구 시 availableQuantity만 증가")
        void restore_increasesAvailableOnly() {
            ProductStock stock = createStock(100, 10);
            stock.reserve(40); // available = 60

            stock.restore(40);

            log.info("[ProductStock.restore] 40개 복구 후 available={}, total={}",
                    stock.getAvailableQuantity(), stock.getTotalQuantity());

            assertThat(stock.getAvailableQuantity()).isEqualTo(100);
            assertThat(stock.getTotalQuantity()).isEqualTo(100);
        }

        @Test
        @DisplayName("복구 후 재고가 회복되면 AVAILABLE 상태로 되돌아감")
        void restore_recoversToAvailableStatus() {
            ProductStock stock = createStock(100, 20);
            stock.reserve(85); // LOW_STOCK

            stock.restore(85);

            log.info("[ProductStock.restore] LOW_STOCK 상태에서 85개 복구 후 status={}", stock.getStatus());

            assertThat(stock.getStatus()).isEqualTo(StockStatus.AVAILABLE);
        }
    }
    @Nested
    @DisplayName("adjustQuantity()")
    class AdjustQuantity {

        @Test
        @DisplayName("총 수량과 가용 수량을 지정한 값으로 갱신")
        void adjustQuantity_setsExactValues() {
            ProductStock stock = createStock(100, 10);

            stock.adjustQuantity(50, 30);

            log.info("[ProductStock.adjustQuantity] (50, 30)으로 조정 후 total={}, available={}",
                    stock.getTotalQuantity(), stock.getAvailableQuantity());

            assertThat(stock.getTotalQuantity()).isEqualTo(50);
            assertThat(stock.getAvailableQuantity()).isEqualTo(30);
        }
    }

    @Nested
    @DisplayName("softDelete()")
    class SoftDelete {

        @Test
        @DisplayName("예약 중인 수량이 없으면 정상적으로 삭제")
        void softDelete_noReservedQuantity_success() {
            ProductStock stock = createStock(100, 10);

            stock.softDelete("tester");

            log.info("[ProductStock.softDelete] 예약 중인 수량 없이 삭제 -> isDeleted={}", stock.isDeleted());

            assertThat(stock.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("예약 중인 수량이 있으면 삭제할 수 없음")
        void softDelete_withReservedQuantity_throwsException() {
            ProductStock stock = createStock(100, 10);
            stock.reserve(30); // total(100) != available(70) -> 예약 중 30

            log.info("[ProductStock.softDelete] 예약 중인 수량 30이 남아있는 상태에서 삭제 시도 -> 삭제 불가 예외 기대");

            assertThatThrownBy(() -> stock.softDelete("tester"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_DELETE_NOT_ALLOWED);
        }
    }
}
