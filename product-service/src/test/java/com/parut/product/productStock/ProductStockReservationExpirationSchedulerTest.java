package com.parut.product.productStock;


import com.parut.product.product.application.stock.scheduler.ProductStockReservationExpirationProcessor;
import com.parut.product.product.application.stock.scheduler.ProductStockReservationExpirationScheduler;
import com.parut.product.product.domain.stock.entity.ProductStockReservation;
import com.parut.product.product.domain.stock.enums.ReservationStatus;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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
            given(productStockReservationRepository.findByStatusAndExpiresAtBefore(
                    eq(ReservationStatus.RESERVED), any(Instant.class), any(Pageable.class)))
                    .willReturn(Page.empty());

            scheduler.expireReservations();

            verify(productStockReservationExpirationProcessor, never()).expireOneReservation(any());
        }

        @Test
        @DisplayName("조회된 모든 만료 예약에 대해 processor를 각각 호출")
        void expireReservations_callsProcessorForEachReservation() {
            ProductStockReservation r1 = createReservation();
            ProductStockReservation r2 = createReservation();
            Page<ProductStockReservation> page = new PageImpl<>(List.of(r1, r2));

            given(productStockReservationRepository.findByStatusAndExpiresAtBefore(
                    eq(ReservationStatus.RESERVED), any(Instant.class), any(Pageable.class)))
                    .willReturn(page);

            scheduler.expireReservations();

            verify(productStockReservationExpirationProcessor).expireOneReservation(r1.getId());
            verify(productStockReservationExpirationProcessor).expireOneReservation(r2.getId());
        }

        @Test
        @DisplayName("한 건 처리 중 예외가 발생해도 나머지 건은 계속 처리")
        void expireReservations_oneFailureDoesNotStopOthers() {
            ProductStockReservation r1 = createReservation();
            ProductStockReservation r2 = createReservation();
            ProductStockReservation r3 = createReservation();
            Page<ProductStockReservation> page = new PageImpl<>(List.of(r1, r2, r3));

            given(productStockReservationRepository.findByStatusAndExpiresAtBefore(
                    eq(ReservationStatus.RESERVED), any(Instant.class), any(Pageable.class)))
                    .willReturn(page);

            // 두 번째 건에서만 예외 발생
            doThrow(new RuntimeException("처리 실패"))
                    .when(productStockReservationExpirationProcessor).expireOneReservation(r2.getId());

            scheduler.expireReservations();

            // 실패한 건 이후에도 나머지 건(r3)은 정상적으로 호출되어야 함
            verify(productStockReservationExpirationProcessor).expireOneReservation(r1.getId());
            verify(productStockReservationExpirationProcessor).expireOneReservation(r2.getId());
            verify(productStockReservationExpirationProcessor).expireOneReservation(r3.getId());
        }

        @Test
        @DisplayName("RESERVED 상태와 현재 시각 기준으로 조회")
        void expireReservations_queriesWithCorrectStatusAndTime() {
            given(productStockReservationRepository.findByStatusAndExpiresAtBefore(
                    any(ReservationStatus.class), any(Instant.class), any(Pageable.class)))
                    .willReturn(Page.empty());

            Instant before = Instant.now();
            scheduler.expireReservations();
            Instant after = Instant.now();

            ArgumentCaptor<ReservationStatus> statusCaptor = ArgumentCaptor.forClass(ReservationStatus.class);
            ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

            verify(productStockReservationRepository).findByStatusAndExpiresAtBefore(
                    statusCaptor.capture(), instantCaptor.capture(), pageableCaptor.capture());

            assertThat(statusCaptor.getValue()).isEqualTo(ReservationStatus.RESERVED);
            assertThat(instantCaptor.getValue()).isBetween(before, after);
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
        }
    }
}
