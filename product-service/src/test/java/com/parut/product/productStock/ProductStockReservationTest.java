package com.parut.product.productStock;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.product.domain.stock.entity.ProductStockReservation;
import com.parut.product.product.domain.stock.enums.ReservationStatus;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@Slf4j
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

            log.info("[ProductStockReservation.create] 생성 직후 status={}", reservation.getStatus());

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

            log.info("[ProductStockReservation.confirm] 만료 전 RESERVED 예약 확정 후 status={}", reservation.getStatus());

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        }

        @Test
        @DisplayName("이미 처리된(RESERVED가 아닌) 예약은 확정할 수 없음")
        void confirm_whenAlreadyProcessed_throwsException() {
            ProductStockReservation reservation = createReservation(Instant.now().plusSeconds(1800));
            reservation.confirm();

            log.info("[ProductStockReservation.confirm] 이미 CONFIRMED(status={})인 예약 재확정 시도 -> ALREADY_PROCESSED 예외 기대",
                    reservation.getStatus());

            assertThatThrownBy(reservation::confirm)
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);
        }

        @Test
        @DisplayName("만료 시각이 지난 예약은 확정할 수 없음")
        void confirm_whenExpired_throwsException() {
            ProductStockReservation reservation = createReservation(Instant.now().minusSeconds(1));

            log.info("[ProductStockReservation.confirm] 만료 시각(expiresAt={})이 지난 예약 확정 시도 -> EXPIRED 예외 기대",
                    reservation.getExpiresAt());

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

            log.info("[ProductStockReservation.cancel] RESERVED 예약 취소 후 status={}", reservation.getStatus());

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        }

        @Test
        @DisplayName("만료된 예약도 취소 (confirm과 달리 만료 검증을 하지 않음)")
        void cancel_allowsExpiredReservation() {
            ProductStockReservation reservation = createReservation(Instant.now().minusSeconds(1));

            reservation.cancel();

            log.info("[ProductStockReservation.cancel] 만료된 예약도 취소 허용됨 -> status={}", reservation.getStatus());

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        }

        @Test
        @DisplayName("이미 처리된 예약은 취소할 수 없음")
        void cancel_whenAlreadyProcessed_throwsException() {
            ProductStockReservation reservation = createReservation(Instant.now().plusSeconds(1800));
            reservation.confirm();

            log.info("[ProductStockReservation.cancel] 이미 CONFIRMED(status={})인 예약 취소 시도 -> ALREADY_PROCESSED 예외 기대",
                    reservation.getStatus());

            assertThatThrownBy(reservation::cancel)
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);
        }
    }
    @Nested
    @DisplayName("expire()")
    class Expire {

        @Test
        @DisplayName("RESERVED 상태면 EXPIRED로 전이")
        void expire_success() {
            ProductStockReservation reservation = createReservation(Instant.now().minus(1, ChronoUnit.HOURS));

            reservation.expire();

            log.info("[ProductStockReservation.expire] RESERVED 예약 만료 처리 후 status={}", reservation.getStatus());

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        }

        @Test
        @DisplayName("이미 처리된 예약은 만료 처리할 수 없음 (스케줄러 중복 실행 방지)")
        void expire_whenAlreadyProcessed_throwsException() {
            ProductStockReservation reservation = createReservation(Instant.now().minusSeconds(1));
            reservation.cancel();

            log.info("[ProductStockReservation.expire] 이미 CANCELLED(status={})인 예약 만료 처리 시도 -> ALREADY_PROCESSED 예외 기대",
                    reservation.getStatus());

            assertThatThrownBy(reservation::expire)
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);
        }
    }

    @Nested
    @DisplayName("fail()")
    class Fail {

        @Test
        @DisplayName("RESERVED 상태면 EXPIRATION_FAILED로 전이")
        void fail_success() {
            ProductStockReservation reservation = createReservation(Instant.now().minus(1, ChronoUnit.HOURS));

            reservation.fail();

            log.info("[ProductStockReservation.fail] RESERVED 예약 격리 처리 후 status={}", reservation.getStatus());

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRATION_FAILED);
        }

        @Test
        @DisplayName("이미 처리된(RESERVED가 아닌) 예약은 격리 처리할 수 없음")
        void fail_whenAlreadyProcessed_throwsException() {
            ProductStockReservation reservation = createReservation(Instant.now().minusSeconds(1));
            reservation.cancel();

            log.info("[ProductStockReservation.fail] 이미 CANCELLED(status={})인 예약 격리 처리 시도 -> ALREADY_PROCESSED 예외 기대",
                    reservation.getStatus());

            assertThatThrownBy(reservation::fail)
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);
        }
    }
}
