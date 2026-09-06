package com.parut.product.productStock;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.product.application.stock.service.ProductStockServiceImpl;
import com.parut.product.product.domain.stock.entity.ProductStock;
import com.parut.product.product.domain.stock.entity.ProductStockEventLog;
import com.parut.product.product.domain.stock.entity.ProductStockReservation;
import com.parut.product.product.domain.stock.enums.ReservationStatus;
import com.parut.product.product.domain.stock.enums.StockEventType;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockEventLogRepository;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockRepository;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class ProductStockServiceImplTest {
    @Mock
    private ProductStockRepository productStockRepository;
    @Mock
    private ProductStockReservationRepository productStockReservationRepository;
    @Mock
    private ProductStockEventLogRepository productStockEventLogRepository;

    @InjectMocks
    private ProductStockServiceImpl productStockService;

    private UUID productId;
    private UUID orderId;
    private UUID orderItemId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        orderItemId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("createStock()")
    class CreateStock {

        @Test
        @DisplayName("올바른 값으로 재고를 생성하고 저장")
        void createStock_success() {
            ArgumentCaptor<ProductStock> captor = ArgumentCaptor.forClass(ProductStock.class);

            productStockService.createStock(productId, 100, 10);

            verify(productStockRepository).save(captor.capture());
            ProductStock saved = captor.getValue();
            assertThat(saved.getProductId()).isEqualTo(productId);
            assertThat(saved.getTotalQuantity()).isEqualTo(100);
            assertThat(saved.getAvailableQuantity()).isEqualTo(100);
            assertThat(saved.getLowStockThreshold()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("getStock()")
    class GetStock {

        @Test
        @DisplayName("재고가 존재하면 정상적으로 반환한다")
        void getStock_success() {
            ProductStock stock = ProductStock.create(productId, 100, 10);
            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId))
                    .willReturn(Optional.of(stock));

            ProductStock result = productStockService.getStock(productId);

            assertThat(result).isEqualTo(stock);
        }

        @Test
        @DisplayName("재고가 없으면 NOT_FOUND 예외로 변환된다")
        void getStock_notFound_throwsException() {
            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> productStockService.getStock(productId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getStocks()")
    class GetStocks {

        @Test
        @DisplayName("여러 productId에 대한 재고 목록을 반환한다")
        void getStocks_success() {
            UUID productId2 = UUID.randomUUID();
            List<UUID> productIds = List.of(productId, productId2);
            ProductStock stock1 = ProductStock.create(productId, 100, 10);
            ProductStock stock2 = ProductStock.create(productId2, 50, 5);
            given(productStockRepository.findByProductIdInAndDeletedAtIsNull(productIds))
                    .willReturn(List.of(stock1, stock2));

            List<ProductStock> result = productStockService.getStocks(productIds);

            assertThat(result).containsExactly(stock1, stock2);
            assertThat(result).extracting(ProductStock::getProductId)
                    .containsExactly(productId, productId2);
        }
    }

    @Nested
    @DisplayName("deleteStock()")
    class DeleteStock {

        @Test
        @DisplayName("재고가 존재하면 softDelete가 호출된다")
        void deleteStock_success() {
            ProductStock stock = ProductStock.create(productId, 100, 10);
            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId))
                    .willReturn(Optional.of(stock));

            productStockService.deleteStock(productId, "tester");

            assertThat(stock.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("재고가 없으면 NOT_FOUND 예외가 발생한다")
        void deleteStock_notFound_throwsException() {
            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> productStockService.deleteStock(productId, "tester"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_NOT_FOUND);
        }

        @Test
        @DisplayName("예약 중인 수량이 있으면 softDelete의 예외가 그대로 전파된다")
        void deleteStock_withReservedQuantity_propagatesException() {
            ProductStock stock = ProductStock.create(productId, 100, 10);
            stock.reserve(30); // 예약 중 30
            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId))
                    .willReturn(Optional.of(stock));

            assertThatThrownBy(() -> productStockService.deleteStock(productId, "tester"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_DELETE_NOT_ALLOWED);
        }
    }

    @Nested
    @DisplayName("getStockList()")
    class GetStockList {

        @Test
        @DisplayName("삭제되지 않은 재고 목록을 페이지 형태로 반환한다")
        void getStockList_success() {
            Pageable pageable = PageRequest.of(0, 10);
            ProductStock stock = ProductStock.create(productId, 100, 10);
            Page<ProductStock> page = new PageImpl<>(List.of(stock));
            given(productStockRepository.findByDeletedAtIsNull(pageable)).willReturn(page);

            Page<ProductStock> result = productStockService.getStockList(pageable);

            assertThat(result.getContent()).containsExactly(stock);
            assertThat(result.getContent().get(0).getProductId()).isEqualTo(productId);
        }
    }

    @Nested
    @DisplayName("updateStock()")
    class UpdateStock {

        @Test
        @DisplayName("재고를 찾을 수 없으면 예외가 발생한다")
        void updateStock_notFound_throwsException() {
            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> productStockService.updateStock(productId, 100))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_NOT_FOUND);
        }

        @Test
        @DisplayName("새 총수량이 예약 중인 수량보다 적으면 예외가 발생한다")
        void updateStock_belowReservedQuantity_throwsException() {
            ProductStock stock = ProductStock.create(productId, 100, 10);
            stock.reserve(30); // reserved = 30
            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId))
                    .willReturn(Optional.of(stock));

            assertThatThrownBy(() -> productStockService.updateStock(productId, 20))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_INVALID_QUANTITY);
        }

        @Test
        @DisplayName("낙관적 락 충돌 시 CONFLICT 에러로 변환된다")
        void updateStock_optimisticLockFailure_convertsToConflictError() {
            ProductStock stock = ProductStock.create(productId, 100, 10);
            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId))
                    .willReturn(Optional.of(stock));
            given(productStockRepository.saveAndFlush(any(ProductStock.class)))
                    .willThrow(OptimisticLockingFailureException.class);

            assertThatThrownBy(() -> productStockService.updateStock(productId, 150))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_CONFLICT);
        }
    }

    @Nested
    @DisplayName("reserve()")
    class Reserve {

        @Test
        @DisplayName("이미 처리된 요청이면 아무 것도 하지 않고 반환한다 (멱등성)")
        void reserve_alreadyProcessed_doesNothing() {
            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.RESERVE))
                    .willReturn(Optional.of(mock(ProductStockEventLog.class)));

            productStockService.reserve(productId, orderId, orderItemId, 10);

            verify(productStockRepository, never()).findByProductIdAndDeletedAtIsNull(any());
        }

        @Test
        @DisplayName("정상 예약 시 재고 차감, 예약 생성, 이벤트로그 저장이 모두 실행된다")
        void reserve_success_persistsAll() {
            ProductStock stock = ProductStock.create(productId, 100, 10);
            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.RESERVE))
                    .willReturn(Optional.empty());
            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId))
                    .willReturn(Optional.of(stock));

            productStockService.reserve(productId, orderId, orderItemId, 20);

            verify(productStockRepository).saveAndFlush(stock);
            verify(productStockReservationRepository).save(any(ProductStockReservation.class));
            verify(productStockEventLogRepository).save(any(ProductStockEventLog.class));
        }

        @Test
        @DisplayName("재고 부족 시 예외가 발생하고 예약/로그가 저장되지 않는다")
        void reserve_shortage_doesNotPersistReservation() {
            ProductStock stock = ProductStock.create(productId, 5, 1);
            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.RESERVE))
                    .willReturn(Optional.empty());
            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId))
                    .willReturn(Optional.of(stock));

            assertThatThrownBy(() -> productStockService.reserve(productId, orderId, orderItemId, 10))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_SHORTAGE);

            verify(productStockReservationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("confirm()")
    class Confirm {

        @Test
        @DisplayName("정상 확정 시 예약 상태 변경과 재고 확정이 모두 반영된다")
        void confirm_success() {
            UUID stockId = UUID.randomUUID();
            ProductStockReservation reservation = ProductStockReservation
                    .create(stockId, orderId, 20, Instant.now().plusSeconds(1800));
            ProductStockEventLog reserveLog = ProductStockEventLog.create(UUID.randomUUID(), orderItemId, StockEventType.RESERVE);
            ProductStock stock = ProductStock.create(productId, 100, 10);

            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.CONFIRM))
                    .willReturn(Optional.empty());
            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.RESERVE))
                    .willReturn(Optional.of(reserveLog));
            given(productStockReservationRepository.findById(reserveLog.getReservationId()))
                    .willReturn(Optional.of(reservation));
            given(productStockRepository.findById(stockId)).willReturn(Optional.of(stock));

            productStockService.confirm(productId, orderId, orderItemId);

            verify(productStockReservationRepository).saveAndFlush(reservation);
            verify(productStockRepository).saveAndFlush(stock);
            verify(productStockEventLogRepository).save(any(ProductStockEventLog.class));
        }

        @Test
        @DisplayName("예약에 연결된 상품과 요청 productId가 다르면 예외가 발생한다")
        void confirm_ownershipMismatch_throwsException() {
            UUID stockId = UUID.randomUUID();
            UUID otherProductId = UUID.randomUUID();
            ProductStockReservation reservation = ProductStockReservation
                    .create(stockId, orderId, 20, Instant.now().plusSeconds(1800));
            ProductStockEventLog reserveLog = ProductStockEventLog.create(UUID.randomUUID(), orderItemId, StockEventType.RESERVE);
            ProductStock stock = ProductStock.create(otherProductId, 100, 10); // 다른 상품

            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.CONFIRM))
                    .willReturn(Optional.empty());
            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.RESERVE))
                    .willReturn(Optional.of(reserveLog));
            given(productStockReservationRepository.findById(reserveLog.getReservationId()))
                    .willReturn(Optional.of(reservation));
            given(productStockRepository.findById(stockId)).willReturn(Optional.of(stock));

            assertThatThrownBy(() -> productStockService.confirm(productId, orderId, orderItemId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_NOT_FOUND);
        }

        @Test
        @DisplayName("주문 ID가 예약과 일치하지 않으면 예외가 발생한다")
        void confirm_orderIdMismatch_throwsException() {
            UUID stockId = UUID.randomUUID();
            UUID differentOrderId = UUID.randomUUID();
            ProductStockReservation reservation = ProductStockReservation
                    .create(stockId, differentOrderId, 20, Instant.now().plusSeconds(1800));
            ProductStockEventLog reserveLog = ProductStockEventLog.create(UUID.randomUUID(), orderItemId, StockEventType.RESERVE);

            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.CONFIRM))
                    .willReturn(Optional.empty());
            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.RESERVE))
                    .willReturn(Optional.of(reserveLog));
            given(productStockReservationRepository.findById(reserveLog.getReservationId()))
                    .willReturn(Optional.of(reservation));

            assertThatThrownBy(() -> productStockService.confirm(productId, orderId, orderItemId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_NOT_FOUND);
        }

        @Test
        @DisplayName("예약 낙관적 락 충돌 시 ALREADY_PROCESSED 에러로 변환된다")
        void confirm_reservationOptimisticLockFailure_convertsToError() {
            UUID stockId = UUID.randomUUID();
            ProductStockReservation reservation = ProductStockReservation
                    .create(stockId, orderId, 20, Instant.now().plusSeconds(1800));
            ProductStockEventLog reserveLog = ProductStockEventLog.create(UUID.randomUUID(), orderItemId, StockEventType.RESERVE);

            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.CONFIRM))
                    .willReturn(Optional.empty());
            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.RESERVE))
                    .willReturn(Optional.of(reserveLog));
            given(productStockReservationRepository.findById(reserveLog.getReservationId()))
                    .willReturn(Optional.of(reservation));
            given(productStockReservationRepository.saveAndFlush(any(ProductStockReservation.class)))
                    .willThrow(OptimisticLockingFailureException.class);

            assertThatThrownBy(() -> productStockService.confirm(productId, orderId, orderItemId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);
        }
    }
    @Nested
    @DisplayName("restore()")
    class Restore {

        @Test
        @DisplayName("이미 처리된 요청이면 아무 것도 하지 않고 반환한다 (멱등성)")
        void restore_alreadyProcessed_doesNothing() {
            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.RESTORE))
                    .willReturn(Optional.of(mock(ProductStockEventLog.class)));

            productStockService.restore(productId, orderId, orderItemId);

            verify(productStockReservationRepository, never()).findById(any());
        }

        @Test
        @DisplayName("정상 복구 시 예약 취소, 재고 복구, 이벤트로그 저장이 모두 실행된다")
        void restore_success() {
            UUID stockId = UUID.randomUUID();
            ProductStockReservation reservation = ProductStockReservation
                    .create(stockId, orderId, 20, Instant.now().plusSeconds(1800));
            ProductStockEventLog reserveLog = ProductStockEventLog.create(UUID.randomUUID(), orderItemId, StockEventType.RESERVE);
            ProductStock stock = ProductStock.create(productId, 100, 10);
            stock.reserve(20);

            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.RESTORE))
                    .willReturn(Optional.empty());
            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.RESERVE))
                    .willReturn(Optional.of(reserveLog));
            given(productStockReservationRepository.findById(reserveLog.getReservationId()))
                    .willReturn(Optional.of(reservation));
            given(productStockRepository.findById(stockId)).willReturn(Optional.of(stock));

            productStockService.restore(productId, orderId, orderItemId);

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
            verify(productStockReservationRepository).saveAndFlush(reservation);
            verify(productStockRepository).saveAndFlush(stock);
            verify(productStockEventLogRepository).save(any(ProductStockEventLog.class));
        }

        @Test
        @DisplayName("예약에 연결된 상품과 요청 productId가 다르면 예외가 발생한다")
        void restore_ownershipMismatch_throwsException() {
            UUID stockId = UUID.randomUUID();
            UUID otherProductId = UUID.randomUUID();
            ProductStockReservation reservation = ProductStockReservation
                    .create(stockId, orderId, 20, Instant.now().plusSeconds(1800));
            ProductStockEventLog reserveLog = ProductStockEventLog.create(UUID.randomUUID(), orderItemId, StockEventType.RESERVE);
            ProductStock stock = ProductStock.create(otherProductId, 100, 10);

            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.RESTORE))
                    .willReturn(Optional.empty());
            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.RESERVE))
                    .willReturn(Optional.of(reserveLog));
            given(productStockReservationRepository.findById(reserveLog.getReservationId()))
                    .willReturn(Optional.of(reservation));
            given(productStockRepository.findById(stockId)).willReturn(Optional.of(stock));

            assertThatThrownBy(() -> productStockService.restore(productId, orderId, orderItemId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_NOT_FOUND);
        }

        @Test
        @DisplayName("주문 ID가 예약과 일치하지 않으면 예외가 발생한다")
        void restore_orderIdMismatch_throwsException() {
            UUID stockId = UUID.randomUUID();
            UUID differentOrderId = UUID.randomUUID();
            ProductStockReservation reservation = ProductStockReservation
                    .create(stockId, differentOrderId, 20, Instant.now().plusSeconds(1800));
            ProductStockEventLog reserveLog = ProductStockEventLog.create(UUID.randomUUID(), orderItemId, StockEventType.RESERVE);

            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.RESTORE))
                    .willReturn(Optional.empty());
            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.RESERVE))
                    .willReturn(Optional.of(reserveLog));
            given(productStockReservationRepository.findById(reserveLog.getReservationId()))
                    .willReturn(Optional.of(reservation));

            assertThatThrownBy(() -> productStockService.restore(productId, orderId, orderItemId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_NOT_FOUND);
        }

        @Test
        @DisplayName("예약 낙관적 락 충돌 시 ALREADY_PROCESSED 에러로 변환된다")
        void restore_reservationOptimisticLockFailure_convertsToError() {
            UUID stockId = UUID.randomUUID();
            ProductStockReservation reservation = ProductStockReservation
                    .create(stockId, orderId, 20, Instant.now().plusSeconds(1800));
            ProductStockEventLog reserveLog = ProductStockEventLog.create(UUID.randomUUID(), orderItemId, StockEventType.RESERVE);

            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.RESTORE))
                    .willReturn(Optional.empty());
            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.RESERVE))
                    .willReturn(Optional.of(reserveLog));
            given(productStockReservationRepository.findById(reserveLog.getReservationId()))
                    .willReturn(Optional.of(reservation));
            given(productStockReservationRepository.saveAndFlush(any(ProductStockReservation.class)))
                    .willThrow(OptimisticLockingFailureException.class);

            assertThatThrownBy(() -> productStockService.restore(productId, orderId, orderItemId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);
        }
    }


    @Nested
    @DisplayName("이벤트로그 저장 멱등성")
    class EventLogIdempotency {

        @Test
        @DisplayName("동시 요청으로 UNIQUE 제약이 걸리면 ALREADY_PROCESSED 에러로 변환된다")
        void saveEventLog_uniqueViolation_convertsToAlreadyProcessed() {
            ProductStock stock = ProductStock.create(productId, 100, 10);
            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.RESERVE))
                    .willReturn(Optional.empty());
            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId))
                    .willReturn(Optional.of(stock));
            given(productStockEventLogRepository.save(any(ProductStockEventLog.class)))
                    .willThrow(DataIntegrityViolationException.class);

            assertThatThrownBy(() -> productStockService.reserve(productId, orderId, orderItemId, 10))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);
        }
    }


}
