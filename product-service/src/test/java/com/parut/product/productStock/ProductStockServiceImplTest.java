package com.parut.product.productStock;

import com.parut.product.global.dto.ProductStockAllocateCommand;
import com.parut.product.global.dto.ProductStockAllocateResult;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.product.application.product.manager.ProductStateManager;
import com.parut.product.product.application.product.reader.ProductReader;
import com.parut.product.product.application.stock.service.ProductStockServiceImpl;
import com.parut.product.product.domain.product.AppearanceType;
import com.parut.product.product.domain.product.Product;
import com.parut.product.product.domain.product.ProductCategory;
import com.parut.product.product.domain.product.SaleUnit;
import com.parut.product.product.domain.stock.entity.ProductStock;
import com.parut.product.product.domain.stock.entity.ProductStockEventLog;
import com.parut.product.product.domain.stock.entity.ProductStockReservation;
import com.parut.product.product.domain.stock.enums.ReservationStatus;
import com.parut.product.product.domain.stock.enums.StockEventType;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockEventLogRepository;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockRepository;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockReservationRepository;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


@Slf4j
@ExtendWith(MockitoExtension.class)
public class ProductStockServiceImplTest {
    @Mock
    private ProductStockRepository productStockRepository;
    @Mock
    private ProductStockReservationRepository productStockReservationRepository;
    @Mock
    private ProductStockEventLogRepository productStockEventLogRepository;
    @Mock
    private ProductReader productReader;
    @Mock
    private ProductStateManager productStateManager;
    @InjectMocks
    private ProductStockServiceImpl productStockService;

    // NOTE: @Value 필드는 Mockito가 주입하지 않으므로 테스트에서 직접 넣어준다.
    private static final Duration RESERVATION_TTL = Duration.ofMinutes(5);

    private UUID productId;
    private UUID orderId;
    private UUID orderItemId;
    private UUID sellerId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        orderItemId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        ReflectionTestUtils.setField(productStockService, "reservationTtl", RESERVATION_TTL);
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

            log.info("[ProductStockService.createStock] productId={}, total={}, available={}, lowStockThreshold={}",
                    saved.getProductId(), saved.getTotalQuantity(), saved.getAvailableQuantity(), saved.getLowStockThreshold());

            assertThat(saved.getProductId()).isEqualTo(productId);
            assertThat(saved.getTotalQuantity()).isEqualTo(100);
            assertThat(saved.getAvailableQuantity()).isEqualTo(100);
            assertThat(saved.getLowStockThreshold()).isEqualTo(10);
        }

        @Test
        @DisplayName("이미 살아있는 재고가 있으면 중복 생성 시 예외가 발생")
        void createStock_duplicateProductId_throwsAlreadyExistsException() {
            given(productStockRepository.save(any(ProductStock.class)))
                    .willThrow(DataIntegrityViolationException.class);

            log.info("[ProductStockService.createStock] product_id 중복 저장 시도 -> ALREADY_EXISTS 예외 기대");

            assertThatThrownBy(() -> productStockService.createStock(productId, 100, 10))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_ALREADY_EXISTS);
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

            log.info("[ProductStockService.getStock] productId={} 조회 결과={}", productId, result);

            assertThat(result).isEqualTo(stock);
        }

        @Test
        @DisplayName("재고가 없으면 NOT_FOUND 예외로 변환된다")
        void getStock_notFound_throwsException() {
            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId))
                    .willReturn(Optional.empty());

            log.info("[ProductStockService.getStock] productId={} 재고 없음 -> NOT_FOUND 예외 기대", productId);

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

            log.info("[ProductStockService.getStocks] 요청 productIds={} -> 조회된 productIds={}",
                    productIds, result.stream().map(ProductStock::getProductId).toList());

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

            log.info("[ProductStockService.deleteStock] productId={} 삭제 후 isDeleted={}", productId, stock.isDeleted());

            assertThat(stock.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("재고가 없으면 NOT_FOUND 예외가 발생한다")
        void deleteStock_notFound_throwsException() {
            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId))
                    .willReturn(Optional.empty());

            log.info("[ProductStockService.deleteStock] productId={} 재고 없음 -> NOT_FOUND 예외 기대", productId);

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

            log.info("[ProductStockService.deleteStock] productId={} 예약 중 수량 30 존재 -> 삭제 불가 예외 기대", productId);

            assertThatThrownBy(() -> productStockService.deleteStock(productId, "tester"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_DELETE_NOT_ALLOWED);
        }

        @Test
        @DisplayName("낙관적 락 충돌 시 CONFLICT 에러로 변환된다")
        void deleteStock_optimisticLockFailure_convertsToConflictError() {
            ProductStock stock = ProductStock.create(productId, 100, 10);
            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId))
                    .willReturn(Optional.of(stock));
            given(productStockRepository.saveAndFlush(any(ProductStock.class)))
                    .willThrow(OptimisticLockingFailureException.class);
            log.info("[ProductStockService.deleteStock] 저장 시 낙관적 락 충돌 발생 -> CONFLICT 예외 기대");

            assertThatThrownBy(() -> productStockService.deleteStock(productId, "tester"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_CONFLICT);
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
            given(productReader.getProductIdsBySellerId(sellerId)).willReturn(List.of(productId));
            given(productStockRepository.findByProductIdInAndDeletedAtIsNull(List.of(productId), pageable))
                    .willReturn(page);

            Page<ProductStock> result = productStockService.getStockList(sellerId, pageable);
            log.info("[ProductStockService.getStockList] 조회된 건수={}, 첫 건 productId={}",
                    result.getContent().size(), result.getContent().get(0).getProductId());

            assertThat(result.getContent()).containsExactly(stock);
            assertThat(result.getContent().get(0).getProductId()).isEqualTo(productId);
        }

        @Test
        @DisplayName("소유한 상품이 없으면 빈 목록을 반환한다")
        void getStockList_noOwnedProducts_returnsEmpty() {
            Pageable pageable = PageRequest.of(0, 10);

            given(productReader.getProductIdsBySellerId(sellerId)).willReturn(List.of());
            given(productStockRepository.findByProductIdInAndDeletedAtIsNull(List.of(), pageable))
                    .willReturn(Page.empty());

            Page<ProductStock> result = productStockService.getStockList(sellerId, pageable);

            assertThat(result.getContent()).isEmpty();
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

            log.info("[ProductStockService.updateStock] productId={} 재고 없음 -> NOT_FOUND 예외 기대", productId);

            assertThatThrownBy(() -> productStockService.updateStock(productId,  sellerId, 100))
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
            given(productReader.isOwnedBy(productId, sellerId)).willReturn(true);
            log.info("[ProductStockService.updateStock] 예약 중 수량 30 > 새 총수량 20 -> 잘못된 수량 예외 기대");

            assertThatThrownBy(() -> productStockService.updateStock(productId, sellerId,20))
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
            given(productReader.isOwnedBy(productId, sellerId)).willReturn(true);
            log.info("[ProductStockService.updateStock] 저장 시 낙관적 락 충돌 발생 -> CONFLICT 예외 기대");

            assertThatThrownBy(() -> productStockService.updateStock(productId, sellerId, 150))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_CONFLICT);
        }

        @Test
        @DisplayName("소유하지 않은 상품의 재고는 수정할 수 없다")
        void updateStock_notOwner_throwsForbiddenException() {
            ProductStock stock = ProductStock.create(productId, 100, 10);
            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId))
                    .willReturn(Optional.of(stock));
            given(productReader.isOwnedBy(productId, sellerId)).willReturn(false);

            assertThatThrownBy(() -> productStockService.updateStock(productId, sellerId, 150))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_FORBIDDEN);
        }

        @Test
        @DisplayName("수량을 0으로 줄이면 품절 알림이 호출된다")
        void updateStock_toZero_notifiesSoldOut() {
            ProductStock stock = ProductStock.create(productId, 30, 5);
            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(stock));
            given(productReader.isOwnedBy(productId, sellerId)).willReturn(true);

            productStockService.updateStock(productId, sellerId, 0);

            verify(productStateManager).soldOut(productId);
        }

        @Test
        @DisplayName("품절 상태에서 수량을 늘리면 재입고 알림이 호출된다")
        void updateStock_fromZero_notifiesRestocked() {
            ProductStock stock = ProductStock.create(productId, 30, 5);
            stock.allocate(30); // SOLD_OUT으로 미리 만들어둠
            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(stock));
            given(productReader.isOwnedBy(productId, sellerId)).willReturn(true);

            productStockService.updateStock(productId, sellerId, 20);

            verify(productStateManager).resumeSaleAfterRestock(productId);
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

            log.info("[ProductStockService.reserve] orderItemId={} 이미 RESERVE 로그 존재 -> 재고 조회 없이 종료 기대", orderItemId);

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

            log.info("[ProductStockService.reserve] productId={}, orderItemId={}, quantity=20 예약 성공 -> stock/reservation/eventLog 저장 확인",
                    productId, orderItemId);

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

            log.info("[ProductStockService.reserve] 가용 재고 5 < 요청 수량 10 -> 재고 부족 예외 및 예약 미저장 기대");

            assertThatThrownBy(() -> productStockService.reserve(productId, orderId, orderItemId, 10))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_SHORTAGE);

            verify(productStockReservationRepository, never()).save(any());
        }
        @Test
        @DisplayName("낙관적 락 충돌 + 동시 재시도로 이미 처리됨 -> 멱등 처리(예외 없음)")
        void reserve_optimisticLockFailure_butAlreadyProcessed_doesNothingSilently() {
            ProductStock stock = ProductStock.create(productId, 100, 10);
            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.RESERVE))
                    .willReturn(Optional.empty())  // 첫 번째 체크: 아직 처리 안 됨
                    .willReturn(Optional.of(mock(ProductStockEventLog.class)));  // 두 번째 체크(catch 안): 이미 처리됨
            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId))
                    .willReturn(Optional.of(stock));
            given(productStockRepository.saveAndFlush(any(ProductStock.class)))
                    .willThrow(OptimisticLockingFailureException.class);

            log.info("[ProductStockService.reserve] 낙관적 락 충돌 + 동시 재시도(이미 처리됨) -> 예외 없이 반환 기대");

            productStockService.reserve(productId, orderId, orderItemId, 20);

            verify(productStockReservationRepository, never()).save(any());
            verify(productStockEventLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("낙관적 락 충돌 + 실제로 처리 안 됨 -> CONFLICT 예외")
        void reserve_optimisticLockFailure_notProcessed_throwsConflict() {
            ProductStock stock = ProductStock.create(productId, 100, 10);
            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.RESERVE))
                    .willReturn(Optional.empty());  // 첫 번째, 두 번째 체크 모두 없음
            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId))
                    .willReturn(Optional.of(stock));
            given(productStockRepository.saveAndFlush(any(ProductStock.class)))
                    .willThrow(OptimisticLockingFailureException.class);

            log.info("[ProductStockService.reserve] 낙관적 락 충돌 + 실제 미처리 -> CONFLICT 예외 기대");

            assertThatThrownBy(() -> productStockService.reserve(productId, orderId, orderItemId, 20))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_CONFLICT);
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

            log.info("[ProductStockService.confirm] orderItemId={} 확정 후 reservation.status={}", orderItemId, reservation.getStatus());

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

            log.info("[ProductStockService.confirm] 요청 productId={} != 재고 소유 productId={} -> NOT_FOUND 예외 기대",
                    productId, otherProductId);

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

            log.info("[ProductStockService.confirm] 요청 orderId={} != 예약된 orderId={} -> NOT_FOUND 예외 기대",
                    orderId, differentOrderId);

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

            log.info("[ProductStockService.confirm] 예약 저장 시 낙관적 락 충돌 발생 -> ALREADY_PROCESSED 예외 기대");

            assertThatThrownBy(() -> productStockService.confirm(productId, orderId, orderItemId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);
        }

        @Test
        @DisplayName("확정으로 재고가 0이 되면 품절 알림이 호출된다")
        void confirm_reachesZero_notifiesSoldOut() {
            UUID stockId = UUID.randomUUID();
            ProductStockReservation reservation = ProductStockReservation
                    .create(stockId, orderId, 20, Instant.now().plusSeconds(1800));
            ProductStockEventLog reserveLog = ProductStockEventLog.create(UUID.randomUUID(), orderItemId, StockEventType.RESERVE);
            ProductStock stock = ProductStock.create(productId, 20, 5);   // ← total=20, 예약 수량(20)과 동일하게

            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.CONFIRM))
                    .willReturn(Optional.empty());
            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.RESERVE))
                    .willReturn(Optional.of(reserveLog));
            given(productStockReservationRepository.findById(reserveLog.getReservationId()))
                    .willReturn(Optional.of(reservation));
            given(productStockRepository.findById(stockId)).willReturn(Optional.of(stock));

            productStockService.confirm(productId, orderId, orderItemId);

            verify(productStateManager).soldOut(productId);
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

            log.info("[ProductStockService.restore] orderItemId={} 이미 RESTORE 로그 존재 -> 예약 조회 없이 종료 기대", orderItemId);

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

            log.info("[ProductStockService.restore] orderItemId={} 복구 후 reservation.status={}, stock.available={}",
                    orderItemId, reservation.getStatus(), stock.getAvailableQuantity());

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

            log.info("[ProductStockService.restore] 요청 productId={} != 재고 소유 productId={} -> NOT_FOUND 예외 기대",
                    productId, otherProductId);

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

            log.info("[ProductStockService.restore] 요청 orderId={} != 예약된 orderId={} -> NOT_FOUND 예외 기대",
                    orderId, differentOrderId);

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

            log.info("[ProductStockService.restore] 예약 저장 시 낙관적 락 충돌 발생 -> ALREADY_PROCESSED 예외 기대");

            assertThatThrownBy(() -> productStockService.restore(productId, orderId, orderItemId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);
        }

        @Test
        @DisplayName("예약이 이미 EXPIRED 상태면 재고/로그를 재처리하지 않고 반환한다")
        void restore_reservationExpired_doesNothingSilently() {
            UUID stockId = UUID.randomUUID();
            ProductStockReservation reservation = ProductStockReservation
                    .create(stockId, orderId, 20, Instant.now().plusSeconds(1800));
            reservation.expire(); // 스케줄러가 이미 만료 처리한 상태를 재현
            ProductStockEventLog reserveLog = ProductStockEventLog.create(UUID.randomUUID(), orderItemId, StockEventType.RESERVE);

            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.RESTORE))
                    .willReturn(Optional.empty());
            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.RESERVE))
                    .willReturn(Optional.of(reserveLog));
            given(productStockReservationRepository.findById(reserveLog.getReservationId()))
                    .willReturn(Optional.of(reservation));

            log.info("[ProductStockService.restore] 예약 상태=EXPIRED -> 재고/로그 재처리 없이 반환 기대");

            productStockService.restore(productId, orderId, orderItemId);

            verify(productStockRepository, never()).findById(any());
            verify(productStockEventLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("예약이 EXPIRATION_FAILED 상태면 격리 에러로 응답한다")
        void restore_reservationIsolated_throwsIsolatedError() {
            UUID stockId = UUID.randomUUID();
            ProductStockReservation reservation = ProductStockReservation
                    .create(stockId, orderId, 20, Instant.now().plusSeconds(1800));
            reservation.fail(); // 격리 상태 재현
            ProductStockEventLog reserveLog = ProductStockEventLog.create(UUID.randomUUID(), orderItemId, StockEventType.RESERVE);

            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.RESTORE))
                    .willReturn(Optional.empty());
            given(productStockEventLogRepository.findByOrderItemIdAndEventType(orderItemId, StockEventType.RESERVE))
                    .willReturn(Optional.of(reserveLog));
            given(productStockReservationRepository.findById(reserveLog.getReservationId()))
                    .willReturn(Optional.of(reservation));

            log.info("[ProductStockService.restore] 예약 상태=EXPIRATION_FAILED -> ISOLATED 예외 기대");

            assertThatThrownBy(() -> productStockService.restore(productId, orderId, orderItemId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_ISOLATED);
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

            log.info("[ProductStockService.reserve] 이벤트로그 저장 시 UNIQUE 제약 위반(동시 요청) -> ALREADY_PROCESSED 예외 기대");

            assertThatThrownBy(() -> productStockService.reserve(productId, orderId, orderItemId, 10))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);
        }
    }

    @Nested
    @DisplayName("allocate()")
    class Allocate {

        @Test
        @DisplayName("판매자 본인이 요청하면 정상 할당된다")
        void allocate_bySeller_success() {
            ProductStock stock = ProductStock.create(productId, 100, 10);
            Product product = createOnSaleProduct(sellerId, 5000L);

            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(stock));
            given(productReader.getProduct(productId)).willReturn(product);

            ProductStockAllocateCommand command = new ProductStockAllocateCommand(productId, 30, sellerId, "SELLER");
            ProductStockAllocateResult result = productStockService.allocate(command);

            log.info("[ProductStockService.allocate] 판매자 본인 할당 성공 -> total={}, price={}",
                    stock.getTotalQuantity(), result.price());

            assertThat(result.quantity()).isEqualTo(30);
            assertThat(result.sellerId()).isEqualTo(sellerId);
            assertThat(result.price()).isEqualTo(5000L);
            assertThat(stock.getTotalQuantity()).isEqualTo(70);
        }

        @Test
        @DisplayName("관리자가 요청하면 소유자가 아니어도 할당된다")
        void allocate_byAdmin_success() {
            ProductStock stock = ProductStock.create(productId, 100, 10);
            Product product = createOnSaleProduct(sellerId, 5000L);

            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(stock));
            given(productReader.getProduct(productId)).willReturn(product);

            UUID adminId = UUID.randomUUID();
            ProductStockAllocateCommand command = new ProductStockAllocateCommand(productId, 30, adminId, "ADMIN");

            log.info("[ProductStockService.allocate] 관리자 요청 -> 소유자 아니어도 할당 기대");

            assertThat(productStockService.allocate(command)).isNotNull();
        }

        @Test
        @DisplayName("소유자가 아닌 판매자가 요청하면 예외가 발생")
        void allocate_notOwner_throwsForbidden() {
            Product product = createOnSaleProduct(sellerId, 5000L);

            given(productReader.getProduct(productId)).willReturn(product);

            UUID otherSellerId = UUID.randomUUID();
            ProductStockAllocateCommand command = new ProductStockAllocateCommand(productId, 30, otherSellerId, "SELLER");

            log.info("[ProductStockService.allocate] 소유자 아닌 판매자 요청 -> FORBIDDEN 예외 기대");

            assertThatThrownBy(() -> productStockService.allocate(command))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_FORBIDDEN);
        }

        @Test
        @DisplayName("판매 중이 아닌 상품이면 예외가 발생")
        void allocate_notOnSale_throwsException() {
            Product product = createDraftProduct(sellerId, 5000L); // DRAFT 상태

            given(productReader.getProduct(productId)).willReturn(product);

            ProductStockAllocateCommand command = new ProductStockAllocateCommand(productId, 30, sellerId, "SELLER");

            log.info("[ProductStockService.allocate] 상품 상태=DRAFT -> NOT_ON_SALE 예외 기대");

            assertThatThrownBy(() -> productStockService.allocate(command))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_PRODUCT_NOT_ON_SALE);

            verify(productStockRepository, never()).findByProductIdAndDeletedAtIsNull(any());
        }

        @Test
        @DisplayName("재고가 부족하면 예외가 발생")
        void allocate_shortage_throwsException() {
            ProductStock stock = ProductStock.create(productId, 10, 2);
            Product product = createOnSaleProduct(sellerId, 5000L);

            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(stock));
            given(productReader.getProduct(productId)).willReturn(product);

            ProductStockAllocateCommand command = new ProductStockAllocateCommand(productId, 20, sellerId, "SELLER");

            log.info("[ProductStockService.allocate] 가용 재고(10) < 요청 수량(20) -> SHORTAGE 예외 기대");

            assertThatThrownBy(() -> productStockService.allocate(command))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_SHORTAGE);
        }

        @Test
        @DisplayName("재고가 없으면 예외가 발생")
        void allocate_stockNotFound_throwsException() {
            Product product = createOnSaleProduct(sellerId, 5000L);

            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId)).willReturn(Optional.empty());
            given(productReader.getProduct(productId)).willReturn(product);

            ProductStockAllocateCommand command = new ProductStockAllocateCommand(productId, 30, sellerId, "SELLER");

            log.info("[ProductStockService.allocate] 재고 없음 -> NOT_FOUND 예외 기대");

            assertThatThrownBy(() -> productStockService.allocate(command))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_NOT_FOUND);
        }

        @Test
        @DisplayName("낙관적 락 충돌 시 CONFLICT 에러로 변환")
        void allocate_optimisticLockFailure_throwsConflict() {
            ProductStock stock = ProductStock.create(productId, 100, 10);
            Product product = createOnSaleProduct(sellerId, 5000L);

            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(stock));
            given(productReader.getProduct(productId)).willReturn(product);
            given(productStockRepository.saveAndFlush(any(ProductStock.class))).willThrow(OptimisticLockingFailureException.class);

            ProductStockAllocateCommand command = new ProductStockAllocateCommand(productId, 30, sellerId, "SELLER");

            log.info("[ProductStockService.allocate] 저장 시 낙관적 락 충돌 -> CONFLICT 예외 기대");

            assertThatThrownBy(() -> productStockService.allocate(command))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_CONFLICT);
        }

        @Test
        @DisplayName("할당으로 재고가 0이 되면 품절 알림이 호출")
        void allocate_reachesZero_notifiesSoldOut() {
            ProductStock stock = ProductStock.create(productId, 30, 5);
            Product product = createOnSaleProduct(sellerId, 5000L);

            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(stock));
            given(productReader.getProduct(productId)).willReturn(product);

            ProductStockAllocateCommand command = new ProductStockAllocateCommand(productId, 30, sellerId, "SELLER"); // 전량 할당

            productStockService.allocate(command);

            verify(productStateManager).soldOut(productId);
        }
    }

    private Product createOnSaleProduct(UUID sellerId, long price) {
        Product product = Product.create(
                sellerId,
                ProductCategory.FRUIT,
                "테스트 상품",
                "설명",
                price,
                AppearanceType.NORMAL,
                "충주",
                LocalDate.of(2026, 9, 1),
                SaleUnit.KG,
                new BigDecimal("1.00")
        );
        ReflectionTestUtils.setField(product, "id", productId);
        product.addImage(UUID.randomUUID());
        product.startSale();
        return product;
    }

    private Product createDraftProduct(UUID sellerId, long price) {
        Product product = Product.create(
                sellerId,
                ProductCategory.FRUIT,
                "테스트 상품",
                "설명",
                price,
                AppearanceType.NORMAL,
                "충주",
                LocalDate.of(2026, 9, 1),
                SaleUnit.KG,
                new BigDecimal("1.00")
        );
        ReflectionTestUtils.setField(product, "id", productId);
        return product;
    }

}
