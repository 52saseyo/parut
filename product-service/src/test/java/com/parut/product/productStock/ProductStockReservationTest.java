package com.parut.product.productStock;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.product.domain.stock.entity.ProductStockReservation;
import com.parut.product.product.domain.stock.enums.ReservationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class ProductStockReservationTest {

    private ProductStockReservation createReservation(Instant expiresAt) {
        return ProductStockReservation.create(UUID.randomUUID(), UUID.randomUUID(), 10, expiresAt);
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("생성 시 상태는 RESERVED")
        void create_setsStatusToReserved() {
            ProductStockReservation reservation = createReservation(Instant.now().plusSeconds(1800));

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RESERVED);
        }
    }
    @Nested
    @DisplayName("confirm()")
    class Confirm {

        @Test
        @DisplayName("RESERVED 상태이고 만료 전이면 CONFIRMED로 전이")
        void confirm_success() {
            ProductStockReservation reservation = createReservation(Instant.now().plusSeconds(1800));

            reservation.confirm();

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        }

        @Test
        @DisplayName("이미 처리된(RESERVED가 아닌) 예약은 확정할 수 없음")
        void confirm_whenAlreadyProcessed_throwsException() {
            ProductStockReservation reservation = createReservation(Instant.now().plusSeconds(1800));
            reservation.confirm();

            assertThatThrownBy(reservation::confirm)
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);
        }

        @Test
        @DisplayName("만료 시각이 지난 예약은 확정할 수 없음")
        void confirm_whenExpired_throwsException() {
            ProductStockReservation reservation = createReservation(Instant.now().minusSeconds(1));

            assertThatThrownBy(reservation::confirm)
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_EXPIRED);
        }
    }

    @Nested
    @DisplayName("cancel()")
    class Cancel {

        @Test
        @DisplayName("RESERVED 상태면 취소")
        void cancel_success() {
            ProductStockReservation reservation = createReservation(Instant.now().plusSeconds(1800));

            reservation.cancel();

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        }

        @Test
        @DisplayName("만료된 예약도 취소 (confirm과 달리 만료 검증을 하지 않음)")
        void cancel_allowsExpiredReservation() {
            ProductStockReservation reservation = createReservation(Instant.now().minusSeconds(1));

            reservation.cancel();

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        }

        @Test
        @DisplayName("이미 처리된 예약은 취소할 수 없다")
        void cancel_whenAlreadyProcessed_throwsException() {
            ProductStockReservation reservation = createReservation(Instant.now().plusSeconds(1800));
            reservation.confirm();

            assertThatThrownBy(reservation::cancel)
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);
        }
    }
    @Nested
    @DisplayName("expire()")
    class Expire {

        @Test
        @DisplayName("RESERVED 상태면 EXPIRED로 전이된다")
        void expire_success() {
            ProductStockReservation reservation = createReservation(Instant.now().minus(1, ChronoUnit.HOURS));

            reservation.expire();

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        }

        @Test
        @DisplayName("이미 처리된 예약은 만료 처리할 수 없다 (스케줄러 중복 실행 방지)")
        void expire_whenAlreadyProcessed_throwsException() {
            ProductStockReservation reservation = createReservation(Instant.now().minusSeconds(1));
            reservation.cancel();

            assertThatThrownBy(reservation::expire)
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);
        }
    }
}
