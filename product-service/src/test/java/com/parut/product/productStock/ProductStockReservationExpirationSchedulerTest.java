package com.parut.product.productStock;


import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.product.application.stock.scheduler.ProductStockReservationExpirationProcessor;
import com.parut.product.product.application.stock.scheduler.ProductStockReservationExpirationScheduler;
import com.parut.product.product.domain.stock.entity.ProductStockReservation;
import com.parut.product.product.domain.stock.enums.ReservationStatus;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockReservationRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class ProductStockReservationExpirationSchedulerTest {

    @Mock
    private ProductStockReservationRepository productStockReservationRepository;
    @Mock
    private ProductStockReservationExpirationProcessor productStockReservationExpirationProcessor;

    @InjectMocks
    private ProductStockReservationExpirationScheduler scheduler;

    private ProductStockReservation createReservation() {
        ProductStockReservation reservation = ProductStockReservation.create(
                UUID.randomUUID(), UUID.randomUUID(), 10, Instant.now().minusSeconds(60)
        );
        ReflectionTestUtils.setField(reservation, "id", UUID.randomUUID());
        return reservation;
    }

    @Nested
    @DisplayName("expireReservations()")
    class ExpireReservations {

        @Test
        @DisplayName("만료된 예약이 없으면 조기 반환하고 processor를 호출하지 않음")
        void expireReservations_empty_returnsEarly() {
            given(productStockReservationRepository.findFirstExpiredBatch(
                    eq(ReservationStatus.RESERVED), any(Instant.class), any(Pageable.class)))
                    .willReturn(List.of());

            scheduler.expireReservations();

            log.info("[Scheduler.expireReservations] 만료 대상 없음 -> expireOneReservation 미호출 기대");

            verify(productStockReservationExpirationProcessor, never()).expireOneReservation(any());
        }

        @Test
        @DisplayName("조회된 모든 만료 예약에 대해 processor를 각각 호출")
        void expireReservations_callsProcessorForEachReservation() {
            ProductStockReservation r1 = createReservation();
            ProductStockReservation r2 = createReservation();

            given(productStockReservationRepository.findFirstExpiredBatch(
                    eq(ReservationStatus.RESERVED), any(Instant.class), any(Pageable.class)))
                    .willReturn(List.of(r1, r2));
            given(productStockReservationRepository.findNextExpiredBatchByCursor(
                    eq(ReservationStatus.RESERVED), any(Instant.class),
                    any(Instant.class), any(UUID.class), any(Pageable.class)))
                    .willReturn(List.of());

            scheduler.expireReservations();

            log.info("[Scheduler.expireReservations] 조회된 2건(r1={}, r2={}) 각각 expireOneReservation 호출 기대",
                    r1.getId(), r2.getId());

            verify(productStockReservationExpirationProcessor).expireOneReservation(r1.getId());
            verify(productStockReservationExpirationProcessor).expireOneReservation(r2.getId());
        }

        @Test
        @DisplayName("한 건 처리 중 예상 못한 예외가 발생해도 나머지 건은 계속 처리하고, 해당 건은 격리 처리도 시도됨")
        void expireReservations_oneFailureDoesNotStopOthers() {
            ProductStockReservation r1 = createReservation();
            ProductStockReservation r2 = createReservation();
            ProductStockReservation r3 = createReservation();

            given(productStockReservationRepository.findFirstExpiredBatch(
                    eq(ReservationStatus.RESERVED), any(Instant.class), any(Pageable.class)))
                    .willReturn(List.of(r1, r2, r3));
            given(productStockReservationRepository.findNextExpiredBatchByCursor(
                    eq(ReservationStatus.RESERVED), any(Instant.class),
                    any(Instant.class), any(UUID.class), any(Pageable.class)))
                    .willReturn(List.of());

            // 두 번째 건에서만 예외 발생
            doThrow(new RuntimeException("처리 실패"))
                    .when(productStockReservationExpirationProcessor).expireOneReservation(r2.getId());

            assertThatCode(() -> scheduler.expireReservations())
                    .doesNotThrowAnyException();

            log.info("[Scheduler.expireReservations] r2={} 처리 중 예상 못한 예외 발생 -> 격리 처리도 시도, r1/r3은 계속 처리 기대",
                    r2.getId());

            // BusinessException이 아닌 예외도 격리 안전망 사각지대에 빠지지 않도록 격리 처리를 시도해야 함
            verify(productStockReservationExpirationProcessor).expirationFailed(r2.getId());
            // 실패한 건 이후에도 나머지 건(r3)은 정상적으로 호출되어야 함
            verify(productStockReservationExpirationProcessor).expireOneReservation(r1.getId());
            verify(productStockReservationExpirationProcessor).expireOneReservation(r2.getId());
            verify(productStockReservationExpirationProcessor).expireOneReservation(r3.getId());
        }

        @Test
        @DisplayName("RESERVED 상태와 고정된 cutoff 시각 기준으로 첫 페이지를 조회")
        void expireReservations_queriesWithCorrectStatusAndTime() {
            given(productStockReservationRepository.findFirstExpiredBatch(
                    any(ReservationStatus.class), any(Instant.class), any(Pageable.class)))
                    .willReturn(List.of());

            Instant before = Instant.now();
            scheduler.expireReservations();
            Instant after = Instant.now();

            ArgumentCaptor<ReservationStatus> statusCaptor = ArgumentCaptor.forClass(ReservationStatus.class);
            ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

            verify(productStockReservationRepository).findFirstExpiredBatch(
                    statusCaptor.capture(), instantCaptor.capture(), pageableCaptor.capture());

            log.info("[Scheduler.expireReservations] 조회 조건 status={}, now={}, pageSize={}",
                    statusCaptor.getValue(), instantCaptor.getValue(), pageableCaptor.getValue().getPageSize());

            assertThat(statusCaptor.getValue()).isEqualTo(ReservationStatus.RESERVED);
            assertThat(instantCaptor.getValue()).isBetween(before, after);
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
        }
    }
    @Test
    @DisplayName("여러 페이지에 걸쳐 조회해도 동일한 cutoff 시각을 사용")
    void expireReservations_usesSameCutoffAcrossPages() {
        ProductStockReservation r1 = createReservation();

        given(productStockReservationRepository.findFirstExpiredBatch(
                eq(ReservationStatus.RESERVED), any(Instant.class), any(Pageable.class)))
                .willReturn(List.of(r1));
        given(productStockReservationRepository.findNextExpiredBatchByCursor(
                eq(ReservationStatus.RESERVED), any(Instant.class),
                any(Instant.class), any(UUID.class), any(Pageable.class)))
                .willReturn(List.of());

        scheduler.expireReservations();

        ArgumentCaptor<Instant> firstCallInstant = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> secondCallInstant = ArgumentCaptor.forClass(Instant.class);

        verify(productStockReservationRepository).findFirstExpiredBatch(
                any(), firstCallInstant.capture(), any());
        verify(productStockReservationRepository).findNextExpiredBatchByCursor(
                any(), secondCallInstant.capture(), any(), any(), any());

        log.info("[Scheduler.expireReservations] 1차 cutoff={}, 2차 cutoff={} 동일 여부 확인",
                firstCallInstant.getValue(), secondCallInstant.getValue());

        assertThat(firstCallInstant.getValue()).isEqualTo(secondCallInstant.getValue());
    }

    @Test
    @DisplayName("NOT_FOUND 예외 발생 시 격리 처리 호출됨")
    void expireReservations_notFound_marksAsExpirationFailed() {
        ProductStockReservation r1 = createReservation();
        given(productStockReservationRepository.findFirstExpiredBatch(
                eq(ReservationStatus.RESERVED), any(Instant.class), any(Pageable.class)))
                .willReturn(List.of(r1));
        given(productStockReservationRepository.findNextExpiredBatchByCursor(
                eq(ReservationStatus.RESERVED), any(Instant.class),
                any(Instant.class), any(UUID.class), any(Pageable.class)))
                .willReturn(List.of());

        doThrow(new BusinessException(ErrorCode.PRODUCT_STOCK_RESERVATION_NOT_FOUND))
                .when(productStockReservationExpirationProcessor).expireOneReservation(r1.getId());

        scheduler.expireReservations();

        log.info("[Scheduler.expireReservations] r1={} NOT_FOUND 예외 발생 -> expirationFailed 호출 기대", r1.getId());

        verify(productStockReservationExpirationProcessor).expirationFailed(r1.getId());
    }

    @Test
    @DisplayName("ALREADY_PROCESSED도 BusinessException이므로 격리 처리 시도 (실제 필터링은 expirationFailed 내부 상태 가드에서 이뤄짐)")
    void expireReservations_alreadyProcessed_alsoAttemptsExpirationFailed() {
        ProductStockReservation r1 = createReservation();
        given(productStockReservationRepository.findFirstExpiredBatch(
                eq(ReservationStatus.RESERVED), any(Instant.class), any(Pageable.class)))
                .willReturn(List.of(r1));
        given(productStockReservationRepository.findNextExpiredBatchByCursor(
                eq(ReservationStatus.RESERVED), any(Instant.class),
                any(Instant.class), any(UUID.class), any(Pageable.class)))
                .willReturn(List.of());

        doThrow(new BusinessException(ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED))
                .when(productStockReservationExpirationProcessor).expireOneReservation(r1.getId());

        scheduler.expireReservations();

        log.info("[Scheduler.expireReservations] r1={} ALREADY_PROCESSED 예외 발생 -> expirationFailed 호출은 시도됨(내부에서 필터링)", r1.getId());

        verify(productStockReservationExpirationProcessor).expirationFailed(r1.getId());
    }

    @Test
    @DisplayName("expirationFailed() 자체가 실패해도 예외가 밖으로 전파되지 않음")
    void expireReservations_expirationFailedItselfThrows_doesNotPropagate() {
        ProductStockReservation r1 = createReservation();
        ProductStockReservation r2 = createReservation();

        given(productStockReservationRepository.findFirstExpiredBatch(
                eq(ReservationStatus.RESERVED), any(Instant.class), any(Pageable.class)))
                .willReturn(List.of(r1, r2));
        given(productStockReservationRepository.findNextExpiredBatchByCursor(
                eq(ReservationStatus.RESERVED), any(Instant.class),
                any(Instant.class), any(UUID.class), any(Pageable.class)))
                .willReturn(List.of());

        doThrow(new BusinessException(ErrorCode.PRODUCT_STOCK_RESERVATION_NOT_FOUND))
                .when(productStockReservationExpirationProcessor).expireOneReservation(r1.getId());
        doThrow(new RuntimeException("격리 처리 중 낙관적 락 충돌"))
                .when(productStockReservationExpirationProcessor).expirationFailed(r1.getId());

        assertThatCode(() -> scheduler.expireReservations())
                .doesNotThrowAnyException();

        log.info("[Scheduler.expireReservations] r1={} 격리 처리 자체 실패해도 r2={}는 계속 처리 기대", r1.getId(), r2.getId());

        // r1 격리 실패에도 불구하고 r2는 정상 처리되어야 함
        verify(productStockReservationExpirationProcessor).expireOneReservation(r2.getId());
    }
}
