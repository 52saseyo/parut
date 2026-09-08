package com.parut.product.productStock;


import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.product.application.stock.scheduler.ProductStockReservationExpirationProcessor;
import com.parut.product.product.domain.stock.entity.ProductStock;
import com.parut.product.product.domain.stock.entity.ProductStockEventLog;
import com.parut.product.product.domain.stock.entity.ProductStockReservation;
import com.parut.product.product.domain.stock.enums.StockEventType;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockEventLogRepository;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockRepository;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockReservationRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class ProductStockReservationExpirationProcessorTest {
    @Mock
    private ProductStockReservationRepository productStockReservationRepository;
    @Mock
    private ProductStockRepository productStockRepository;
    @Mock
    private ProductStockEventLogRepository productStockEventLogRepository;

    @InjectMocks
    private ProductStockReservationExpirationProcessor processor;

    @Nested
    @DisplayName("expireOneReservation()")
    class ExpireOneReservation {

        @Test
        @DisplayName("정상적으로 만료 처리하면 예약은 EXPIRED, 재고는 복구")
        void expireOneReservation_success() {
            UUID reservationId = UUID.randomUUID();
            UUID stockId = UUID.randomUUID();
            UUID orderItemId = UUID.randomUUID();

            ProductStockReservation reservation = ProductStockReservation
                    .create(stockId, UUID.randomUUID(), 15, Instant.now().minusSeconds(60));
            ReflectionTestUtils.setField(reservation, "id", reservationId);

            ProductStockEventLog reserveLog = ProductStockEventLog.create(reservationId, orderItemId, StockEventType.RESERVE);
            ProductStock stock = ProductStock.create(UUID.randomUUID(), 100, 10);
            stock.reserve(15);

            given(productStockReservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
            given(productStockEventLogRepository.findByReservationIdAndEventType(reservationId, StockEventType.RESERVE))
                    .willReturn(Optional.of(reserveLog));
            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.RESTORE))
                    .willReturn(Optional.empty());
            given(productStockRepository.findById(stockId)).willReturn(Optional.of(stock));

            processor.expireOneReservation(reservationId);

            log.info("[Processor.expireOneReservation] reservationId={} 만료 처리 후 reservation.status={}, stock.available={}",
                    reservationId, reservation.getStatus(), stock.getAvailableQuantity());

            verify(productStockReservationRepository).saveAndFlush(reservation);
            verify(productStockRepository).saveAndFlush(stock);
            verify(productStockEventLogRepository).save(any(ProductStockEventLog.class));
        }

        @Test
        @DisplayName("이미 RESTORE 처리된 예약이면 아무 작업도 하지 않음 (멱등성)")
        void expireOneReservation_alreadyRestored_doesNothing() {
            UUID reservationId = UUID.randomUUID();
            UUID orderItemId = UUID.randomUUID();
            ProductStockReservation reservation = ProductStockReservation
                    .create(UUID.randomUUID(), UUID.randomUUID(), 10, Instant.now().minusSeconds(60));
            ProductStockEventLog reserveLog = ProductStockEventLog.create(reservationId, orderItemId, StockEventType.RESERVE);
            ReflectionTestUtils.setField(reservation, "id", reservationId);

            given(productStockReservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
            given(productStockEventLogRepository.findByReservationIdAndEventType(reservationId, StockEventType.RESERVE))
                    .willReturn(Optional.of(reserveLog));
            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.RESTORE))
                    .willReturn(Optional.of(ProductStockEventLog.create(reservationId, orderItemId, StockEventType.RESTORE)));

            processor.expireOneReservation(reservationId);

            log.info("[Processor.expireOneReservation] reservationId={} 이미 RESTORE 로그 존재 -> 재고 조회 및 저장 없이 종료 기대",
                    reservationId);

            verify(productStockRepository, never()).findById(any());
            verify(productStockReservationRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("예약을 찾을 수 없으면 예외가 발생하고 트랜잭션이 롤백")
        void expireOneReservation_reservationNotFound_throwsException() {
            UUID reservationId = UUID.randomUUID();
            given(productStockReservationRepository.findById(reservationId)).willReturn(Optional.empty());

            log.info("[Processor.expireOneReservation] reservationId={} 예약 자체를 찾을 수 없음 -> NOT_FOUND 예외 기대",
                    reservationId);

            assertThatThrownBy(() -> processor.expireOneReservation(reservationId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_NOT_FOUND);
        }

        @Test
        @DisplayName("이미 CONFIRMED로 처리된 예약이면 expire() 자체에서 예외가 발생 (레이스 컨디션 방어)")
        void expireOneReservation_alreadyConfirmed_throwsException() {
            UUID reservationId = UUID.randomUUID();
            UUID orderItemId = UUID.randomUUID();
            ProductStockReservation reservation = ProductStockReservation
                    .create(UUID.randomUUID(), UUID.randomUUID(), 10, Instant.now().plusSeconds(1800));
            reservation.confirm(); // 이미 CONFIRMED
            ProductStockEventLog reserveLog = ProductStockEventLog.create(reservationId, orderItemId, StockEventType.RESERVE);
            ReflectionTestUtils.setField(reservation, "id", reservationId);

            given(productStockReservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
            given(productStockEventLogRepository.findByReservationIdAndEventType(reservationId, StockEventType.RESERVE))
                    .willReturn(Optional.of(reserveLog));
            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.RESTORE))
                    .willReturn(Optional.empty());

            log.info("[Processor.expireOneReservation] reservationId={} 이미 CONFIRMED(status={}) 상태 -> ALREADY_PROCESSED 예외 기대",
                    reservationId, reservation.getStatus());

            assertThatThrownBy(() -> processor.expireOneReservation(reservationId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);

            verify(productStockRepository, never()).findById(any());
        }

        @Test
        @DisplayName("예약 저장 시 낙관적 락 충돌이 나면 예외를 그대로 던져 트랜잭션을 롤백")
        void expireOneReservation_optimisticLockFailure_throwsWithoutSwallowing() {
            UUID reservationId = UUID.randomUUID();
            UUID orderItemId = UUID.randomUUID();
            ProductStockReservation reservation = ProductStockReservation
                    .create(UUID.randomUUID(), UUID.randomUUID(), 10, Instant.now().minusSeconds(60));
            ProductStockEventLog reserveLog = ProductStockEventLog.create(reservationId, orderItemId, StockEventType.RESERVE);
            ReflectionTestUtils.setField(reservation, "id", reservationId);

            given(productStockReservationRepository.findById(reservationId)).willReturn(Optional.of(reservation));
            given(productStockEventLogRepository.findByReservationIdAndEventType(reservationId, StockEventType.RESERVE))
                    .willReturn(Optional.of(reserveLog));
            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.RESTORE))
                    .willReturn(Optional.empty());
            given(productStockReservationRepository.saveAndFlush(any(ProductStockReservation.class)))
                    .willThrow(OptimisticLockingFailureException.class);

            log.info("[Processor.expireOneReservation] reservationId={} 예약 저장 시 낙관적 락 충돌 -> 예외가 삼켜지지 않고 그대로 전파되어야 함",
                    reservationId);

            // 여기서 try-catch로 삼켜지지 않고 그대로 던져져야 재고 복구(stock.restore)가 실행되지 않음
            assertThatThrownBy(() -> processor.expireOneReservation(reservationId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);

            verify(productStockRepository, never()).findById(any());
        }
    }

}
