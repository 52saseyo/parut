package com.parut.product.productStock;

import com.parut.product.global.dto.ProductStockAllocateCommand;
import com.parut.product.global.dto.ProductStockAllocateResult;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.product.application.authorization.stock.ProductStockAuthorizationChecker;
import com.parut.product.product.application.dto.stock.ProductStockItem;
import com.parut.product.product.application.dto.stock.ProductStockReserveItem;
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
import java.util.ArrayList;
import java.util.Collection;
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
    @Mock
    private ProductStockAuthorizationChecker authorizationChecker;

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
        @DisplayName("삭제되지 않은 재고 목록을 페이지 형태로 반환")
        void getStockList_success() {
            Pageable pageable = PageRequest.of(0, 10);
            ProductStock stock = ProductStock.create(productId, 100, 10);
            Page<ProductStock> page = new PageImpl<>(List.of(stock));
            given(productReader.getProductIdsBySellerId(sellerId)).willReturn(List.of(productId));
            given(productStockRepository.findByProductIdInAndDeletedAtIsNull(List.of(productId), pageable))
                    .willReturn(page);

            Page<ProductStock> result = productStockService.getStockList(sellerId, "SELLER", pageable);
            log.info("[ProductStockService.getStockList] 조회된 건수={}, 첫 건 productId={}",
                    result.getContent().size(), result.getContent().get(0).getProductId());

            assertThat(result.getContent()).containsExactly(stock);
            assertThat(result.getContent().get(0).getProductId()).isEqualTo(productId);
        }

        @Test
        @DisplayName("소유한 상품이 없으면 빈 목록을 반환")
        void getStockList_noOwnedProducts_returnsEmpty() {
            Pageable pageable = PageRequest.of(0, 10);

            given(productReader.getProductIdsBySellerId(sellerId)).willReturn(List.of());
            given(productStockRepository.findByProductIdInAndDeletedAtIsNull(List.of(), pageable))
                    .willReturn(Page.empty());

            Page<ProductStock> result = productStockService.getStockList(sellerId, "SELLER", pageable);
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("관리자가 요청하면 전체 재고 목록을 반환")
        void getStockList_byAdmin_returnsAllStocks() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<ProductStock> page = new PageImpl<>(List.of(ProductStock.create(productId, 100, 10)));
            given(authorizationChecker.isAdmin("ADMIN")).willReturn(true);
            given(productStockRepository.findByDeletedAtIsNull(pageable)).willReturn(page);

            Page<ProductStock> result = productStockService.getStockList(UUID.randomUUID(), "ADMIN", pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(productReader, never()).getProductIdsBySellerId(any());
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

            assertThatThrownBy(() -> productStockService.updateStock(productId, sellerId, "SELLER", 100))
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
            given(productReader.getSellerId(productId)).willReturn(sellerId);
            log.info("[ProductStockService.updateStock] 예약 중 수량 30 > 새 총수량 20 -> 잘못된 수량 예외 기대");

            assertThatThrownBy(() -> productStockService.updateStock(productId, sellerId, "SELLER", 20))
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
            given(productReader.getSellerId(productId)).willReturn(sellerId);
            log.info("[ProductStockService.updateStock] 저장 시 낙관적 락 충돌 발생 -> CONFLICT 예외 기대");

            assertThatThrownBy(() -> productStockService.updateStock(productId, sellerId, "SELLER", 150))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_CONFLICT);
        }

        @Test
        @DisplayName("소유하지 않은 상품의 재고는 수정할 수 없다")
        void updateStock_notOwner_throwsForbiddenException() {
            ProductStock stock = ProductStock.create(productId, 100, 10);
            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId))
                    .willReturn(Optional.of(stock));
            given(productReader.getSellerId(productId)).willReturn(sellerId);
            doThrow(new BusinessException(ErrorCode.PRODUCT_STOCK_FORBIDDEN))
                    .when(authorizationChecker).requireOwnerOrAdmin(any(), any(), any());

            assertThatThrownBy(() -> productStockService.updateStock(productId, sellerId, "SELLER", 150))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_FORBIDDEN);
        }

        @Test
        @DisplayName("수량을 0으로 줄이면 품절 알림이 호출된다")
        void updateStock_toZero_notifiesSoldOut() {
            ProductStock stock = ProductStock.create(productId, 30, 5);
            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(stock));
            given(productReader.getSellerId(productId)).willReturn(sellerId);
            productStockService.updateStock(productId, sellerId, "SELLER", 0);
            verify(productStateManager).soldOut(productId);
        }

        @Test
        @DisplayName("품절 상태에서 수량을 늘리면 재입고 알림이 호출된다")
        void updateStock_fromZero_notifiesRestocked() {
            ProductStock stock = ProductStock.create(productId, 30, 5);
            stock.allocate(30); // SOLD_OUT으로 미리 만들어둠
            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(stock));
            given(productReader.getSellerId(productId)).willReturn(sellerId);
            productStockService.updateStock(productId, sellerId, "SELLER", 20);
            verify(productStateManager).resumeSaleAfterRestock(productId);
        }
    }

    @Nested
    @DisplayName("reserve()")
    class Reserve {

        @Test
        @DisplayName("이미 처리된 항목은 건너뛰고, 나머지 항목은 정상 처리")
        void reserve_alreadyProcessedItemSkipped_othersProcessed() {
            UUID productId2 = UUID.randomUUID();
            UUID orderItemId2 = UUID.randomUUID();
            ProductStock stock2 = ProductStock.create(productId2, 100, 10);

            ProductStockEventLog alreadyProcessedLog =
                    ProductStockEventLog.create(UUID.randomUUID(), orderItemId, StockEventType.RESERVE);

            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId, orderItemId2), StockEventType.RESERVE))
                    .willReturn(List.of(alreadyProcessedLog)); // orderItemId만 이미 처리됨

            given(productStockRepository.findByProductIdInAndDeletedAtIsNull(List.of(productId2)))
                    .willReturn(List.of(stock2));

            List<ProductStockReserveItem> items = List.of(
                    new ProductStockReserveItem(productId, orderItemId, 10),
                    new ProductStockReserveItem(productId2, orderItemId2, 20)
            );

            productStockService.reserve(orderId, items);

            log.info("[ProductStockService.reserve] orderItemId={}(이미 처리) 건너뜀, orderItemId2={} 정상 처리 기대",
                    orderItemId, orderItemId2);

            verify(productStockRepository, never())
                    .findByProductIdInAndDeletedAtIsNull(List.of(productId));

            ArgumentCaptor<Collection<ProductStock>> stockCaptor = ArgumentCaptor.forClass(Collection.class);
            verify(productStockRepository).saveAllAndFlush(stockCaptor.capture());
            assertThat(stockCaptor.getValue()).containsExactly(stock2);

            verify(productStockReservationRepository).saveAllAndFlush(anyCollection());
        }

        @Test
        @DisplayName("여러 항목을 정상 예약하면 항목 수만큼 재고 차감, 예약 생성, 이벤트로그 저장이 실행")
        void reserve_bulkSuccess_persistsAllItems() {
            UUID productId2 = UUID.randomUUID();
            UUID orderItemId2 = UUID.randomUUID();
            ProductStock stock1 = ProductStock.create(productId, 100, 10);
            ProductStock stock2 = ProductStock.create(productId2, 50, 5);

            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId, orderItemId2), StockEventType.RESERVE))
                    .willReturn(List.of());
            given(productStockRepository.findByProductIdInAndDeletedAtIsNull(List.of(productId, productId2)))
                    .willReturn(List.of(stock1, stock2));

            List<ProductStockReserveItem> items = List.of(
                    new ProductStockReserveItem(productId, orderItemId, 20),
                    new ProductStockReserveItem(productId2, orderItemId2, 10)
            );

            productStockService.reserve(orderId, items);

            log.info("[ProductStockService.reserve] 벌크 예약 성공 -> 2건 모두 저장 확인");

            ArgumentCaptor<Collection<ProductStock>> stockCaptor = ArgumentCaptor.forClass(Collection.class);
            verify(productStockRepository).saveAllAndFlush(stockCaptor.capture());
            assertThat(stockCaptor.getValue()).containsExactlyInAnyOrder(stock1, stock2);

            ArgumentCaptor<Collection<ProductStockReservation>> reservationCaptor = ArgumentCaptor.forClass(Collection.class);
            verify(productStockReservationRepository).saveAllAndFlush(reservationCaptor.capture());
            assertThat(reservationCaptor.getValue()).hasSize(2);

            ArgumentCaptor<Collection<ProductStockEventLog>> logCaptor = ArgumentCaptor.forClass(Collection.class);
            verify(productStockEventLogRepository).saveAllAndFlush(logCaptor.capture());
            assertThat(logCaptor.getValue()).hasSize(2);
        }

        @Test
        @DisplayName("두 번째 항목에서 재고 부족 발생 시 예외가 전파되고 이후 항목은 처리되지 않음")
        void reserve_bulkFailure_stopsProcessingRemainingItems() {
            UUID productId2 = UUID.randomUUID();
            UUID productId3 = UUID.randomUUID();
            UUID orderItemId2 = UUID.randomUUID();
            UUID orderItemId3 = UUID.randomUUID();

            ProductStock stock1 = ProductStock.create(productId, 100, 10);
            ProductStock stock2 = ProductStock.create(productId2, 5, 1); // 재고 부족 유발

            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId, orderItemId2, orderItemId3), StockEventType.RESERVE))
                    .willReturn(List.of());
            given(productStockRepository.findByProductIdInAndDeletedAtIsNull(
                    List.of(productId, productId2, productId3)))
                    .willReturn(List.of(stock1, stock2)); // productId3에 대한 재고는 없음(안 쓰임)

            List<ProductStockReserveItem> items = List.of(
                    new ProductStockReserveItem(productId, orderItemId, 10),
                    new ProductStockReserveItem(productId2, orderItemId2, 10), // 재고 5 < 요청 10 -> 실패
                    new ProductStockReserveItem(productId3, orderItemId3, 10)
            );

            log.info("[ProductStockService.reserve] 두 번째 항목 재고 부족 -> 예외 전파 및 저장 미실행 기대");

            assertThatThrownBy(() -> productStockService.reserve(orderId, items))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_SHORTAGE);

            // 재고 부족은 메모리 단계에서 터지므로 저장 자체가 호출되지 않아야 함(all-or-nothing)
            verify(productStockRepository, never()).saveAllAndFlush(any());
        }

        @Test
        @DisplayName("낙관적 락 충돌 시 CONFLICT 예외")
        void reserve_optimisticLockFailure_throwsConflict() {
            ProductStock stock = ProductStock.create(productId, 100, 10);
            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId), StockEventType.RESERVE))
                    .willReturn(List.of());
            given(productStockRepository.findByProductIdInAndDeletedAtIsNull(List.of(productId)))
                    .willReturn(List.of(stock));
            given(productStockRepository.saveAllAndFlush(anyCollection()))
                    .willThrow(OptimisticLockingFailureException.class);

            List<ProductStockReserveItem> items = List.of(
                    new ProductStockReserveItem(productId, orderItemId, 20)
            );

            log.info("[ProductStockService.reserve] 낙관적 락 충돌 -> CONFLICT 예외 기대");

            assertThatThrownBy(() -> productStockService.reserve(orderId, items))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_CONFLICT);

            verify(productStockReservationRepository, never()).saveAllAndFlush(any());
        }

        @Test
        @DisplayName("여러 항목이 동일한 만료 시각을 사용한다")
        void reserve_bulkItems_useSameExpiresAt() {
            UUID productId2 = UUID.randomUUID();
            UUID orderItemId2 = UUID.randomUUID();
            ProductStock stock1 = ProductStock.create(productId, 100, 10);
            ProductStock stock2 = ProductStock.create(productId2, 50, 5);

            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId, orderItemId2), StockEventType.RESERVE))
                    .willReturn(List.of());
            given(productStockRepository.findByProductIdInAndDeletedAtIsNull(List.of(productId, productId2)))
                    .willReturn(List.of(stock1, stock2));

            List<ProductStockReserveItem> items = List.of(
                    new ProductStockReserveItem(productId, orderItemId, 10),
                    new ProductStockReserveItem(productId2, orderItemId2, 10)
            );

            productStockService.reserve(orderId, items);

            ArgumentCaptor<Collection<ProductStockReservation>> captor = ArgumentCaptor.forClass(Collection.class);
            verify(productStockReservationRepository).saveAllAndFlush(captor.capture());

            List<ProductStockReservation> saved = new ArrayList<>(captor.getValue());
            log.info("[ProductStockService.reserve] 항목1 expiresAt={}, 항목2 expiresAt={}",
                    saved.get(0).getExpiresAt(), saved.get(1).getExpiresAt());

            assertThat(saved).hasSize(2);
            assertThat(saved.get(0).getExpiresAt()).isEqualTo(saved.get(1).getExpiresAt());
        }
    }

    @Nested
    @DisplayName("confirm()")
    class Confirm {

        @Test
        @DisplayName("정상 확정 시 예약 상태 변경과 재고 확정이 모두 반영된다")
        void confirm_success() {
            UUID reservationId = UUID.randomUUID();
            UUID stockId = UUID.randomUUID();
            ProductStockReservation reservation = ProductStockReservation
                    .create(stockId, orderId, 20, Instant.now().plusSeconds(1800));
            ReflectionTestUtils.setField(reservation, "id", reservationId);
            ProductStockEventLog reserveLog = ProductStockEventLog.create(reservationId, orderItemId, StockEventType.RESERVE);
            ProductStock stock = ProductStock.create(productId, 100, 10);
            ReflectionTestUtils.setField(stock, "id", stockId);

            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId), StockEventType.CONFIRM))
                    .willReturn(List.of());
            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId), StockEventType.RESERVE))
                    .willReturn(List.of(reserveLog));
            given(productStockReservationRepository.findAllById(List.of(reservationId)))
                    .willReturn(List.of(reservation));
            given(productStockRepository.findAllById(List.of(stockId)))
                    .willReturn(List.of(stock));

            List<ProductStockItem> items = List.of(new ProductStockItem(productId, orderItemId));

            productStockService.confirm(orderId, items);

            log.info("[ProductStockService.confirm] orderItemId={} 확정 후 reservation.status={}", orderItemId, reservation.getStatus());

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
            verify(productStockReservationRepository).saveAllAndFlush(anyCollection());
            verify(productStockRepository).saveAllAndFlush(anyCollection());

            ArgumentCaptor<Collection<ProductStockEventLog>> logCaptor = ArgumentCaptor.forClass(Collection.class);
            verify(productStockEventLogRepository).saveAllAndFlush(logCaptor.capture());
            assertThat(logCaptor.getValue()).hasSize(1);
            assertThat(logCaptor.getValue().iterator().next().getOrderItemId()).isEqualTo(orderItemId);
        }

        @Test
        @DisplayName("이미 처리된 항목은 건너뛰고, 나머지 항목은 정상 처리된다")
        void confirm_alreadyProcessedItemSkipped_othersProcessed() {
            UUID productId2 = UUID.randomUUID();
            UUID orderItemId2 = UUID.randomUUID();
            UUID reservationId2 = UUID.randomUUID();
            UUID stockId2 = UUID.randomUUID();
            ProductStockReservation reservation2 = ProductStockReservation
                    .create(stockId2, orderId, 15, Instant.now().plusSeconds(1800));
            ReflectionTestUtils.setField(reservation2, "id", reservationId2);
            ProductStockEventLog reserveLog2 = ProductStockEventLog.create(reservationId2, orderItemId2, StockEventType.RESERVE);
            ProductStock stock2 = ProductStock.create(productId2, 50, 5);
            ReflectionTestUtils.setField(stock2, "id", stockId2);

            ProductStockEventLog alreadyConfirmedLog =
                    ProductStockEventLog.create(UUID.randomUUID(), orderItemId, StockEventType.CONFIRM);

            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId, orderItemId2), StockEventType.CONFIRM))
                    .willReturn(List.of(alreadyConfirmedLog)); // orderItemId만 이미 처리됨
            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId2), StockEventType.RESERVE))
                    .willReturn(List.of(reserveLog2));
            given(productStockReservationRepository.findAllById(List.of(reservationId2)))
                    .willReturn(List.of(reservation2));
            given(productStockRepository.findAllById(List.of(stockId2)))
                    .willReturn(List.of(stock2));

            List<ProductStockItem> items = List.of(
                    new ProductStockItem(productId, orderItemId),
                    new ProductStockItem(productId2, orderItemId2)
            );

            productStockService.confirm(orderId, items);

            log.info("[ProductStockService.confirm] orderItemId={}(이미 처리) 건너뜀, orderItemId2={} 정상 처리 기대",
                    orderItemId, orderItemId2);

            verify(productStockEventLogRepository, never())
                    .findByOrderItemIdInAndEventType(List.of(orderItemId), StockEventType.RESERVE);

            ArgumentCaptor<Collection<ProductStock>> stockCaptor = ArgumentCaptor.forClass(Collection.class);
            verify(productStockRepository).saveAllAndFlush(stockCaptor.capture());
            assertThat(stockCaptor.getValue()).containsExactly(stock2);
        }

        @Test
        @DisplayName("예약에 연결된 상품과 요청 productId가 다르면 예외가 발생한다")
        void confirm_ownershipMismatch_throwsException() {
            UUID reservationId = UUID.randomUUID();
            UUID stockId = UUID.randomUUID();
            UUID otherProductId = UUID.randomUUID();
            ProductStockReservation reservation = ProductStockReservation
                    .create(stockId, orderId, 20, Instant.now().plusSeconds(1800));
            ReflectionTestUtils.setField(reservation, "id", reservationId);
            ProductStockEventLog reserveLog = ProductStockEventLog.create(reservationId, orderItemId, StockEventType.RESERVE);
            ProductStock stock = ProductStock.create(otherProductId, 100, 10);
            ReflectionTestUtils.setField(stock, "id", stockId);

            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId), StockEventType.CONFIRM))
                    .willReturn(List.of());
            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId), StockEventType.RESERVE))
                    .willReturn(List.of(reserveLog));
            given(productStockReservationRepository.findAllById(List.of(reservationId)))
                    .willReturn(List.of(reservation));
            given(productStockRepository.findAllById(List.of(stockId)))
                    .willReturn(List.of(stock));

            List<ProductStockItem> items = List.of(new ProductStockItem(productId, orderItemId));

            log.info("[ProductStockService.confirm] 요청 productId={} != 재고 소유 productId={} -> RESERVATION_NOT_FOUND 예외 기대",
                    productId, otherProductId);

            assertThatThrownBy(() -> productStockService.confirm(orderId, items))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_NOT_FOUND);
        }

        @Test
        @DisplayName("주문 ID가 예약과 일치하지 않으면 예외가 발생한다")
        void confirm_orderIdMismatch_throwsException() {
            UUID reservationId = UUID.randomUUID();
            UUID stockId = UUID.randomUUID();
            UUID differentOrderId = UUID.randomUUID();
            ProductStockReservation reservation = ProductStockReservation
                    .create(stockId, differentOrderId, 20, Instant.now().plusSeconds(1800));
            ReflectionTestUtils.setField(reservation, "id", reservationId);
            ProductStockEventLog reserveLog = ProductStockEventLog.create(reservationId, orderItemId, StockEventType.RESERVE);

            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId), StockEventType.CONFIRM))
                    .willReturn(List.of());
            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId), StockEventType.RESERVE))
                    .willReturn(List.of(reserveLog));
            given(productStockReservationRepository.findAllById(List.of(reservationId)))
                    .willReturn(List.of(reservation));

            List<ProductStockItem> items = List.of(new ProductStockItem(productId, orderItemId));

            log.info("[ProductStockService.confirm] 요청 orderId={} != 예약된 orderId={} -> NOT_FOUND 예외 기대",
                    orderId, differentOrderId);

            assertThatThrownBy(() -> productStockService.confirm(orderId, items))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_NOT_FOUND);
        }

        @Test
        @DisplayName("예약/재고 저장 시 낙관적 락 충돌이 발생하면 ALREADY_PROCESSED 에러로 변환된다")
        void confirm_optimisticLockFailure_convertsToError() {
            UUID reservationId = UUID.randomUUID();
            UUID stockId = UUID.randomUUID();
            ProductStockReservation reservation = ProductStockReservation
                    .create(stockId, orderId, 20, Instant.now().plusSeconds(1800));
            ReflectionTestUtils.setField(reservation, "id", reservationId);
            ProductStockEventLog reserveLog = ProductStockEventLog.create(reservationId, orderItemId, StockEventType.RESERVE);
            ProductStock stock = ProductStock.create(productId, 100, 10);
            ReflectionTestUtils.setField(stock, "id", stockId);

            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId), StockEventType.CONFIRM))
                    .willReturn(List.of());
            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId), StockEventType.RESERVE))
                    .willReturn(List.of(reserveLog));
            given(productStockReservationRepository.findAllById(List.of(reservationId)))
                    .willReturn(List.of(reservation));
            given(productStockRepository.findAllById(List.of(stockId)))
                    .willReturn(List.of(stock));
            given(productStockReservationRepository.saveAllAndFlush(anyCollection()))
                    .willThrow(OptimisticLockingFailureException.class);

            List<ProductStockItem> items = List.of(new ProductStockItem(productId, orderItemId));

            log.info("[ProductStockService.confirm] 예약 저장 시 낙관적 락 충돌 발생 -> ALREADY_PROCESSED 예외 기대");

            assertThatThrownBy(() -> productStockService.confirm(orderId, items))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);
        }

        @Test
        @DisplayName("확정으로 재고가 0이 되면 품절 알림이 호출된다")
        void confirm_reachesZero_notifiesSoldOut() {
            UUID reservationId = UUID.randomUUID();
            UUID stockId = UUID.randomUUID();
            ProductStockReservation reservation = ProductStockReservation
                    .create(stockId, orderId, 20, Instant.now().plusSeconds(1800));
            ReflectionTestUtils.setField(reservation, "id", reservationId);
            ProductStockEventLog reserveLog = ProductStockEventLog.create(reservationId, orderItemId, StockEventType.RESERVE);
            ProductStock stock = ProductStock.create(productId, 20, 5);
            ReflectionTestUtils.setField(stock, "id", stockId);

            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId), StockEventType.CONFIRM))
                    .willReturn(List.of());
            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId), StockEventType.RESERVE))
                    .willReturn(List.of(reserveLog));
            given(productStockReservationRepository.findAllById(List.of(reservationId)))
                    .willReturn(List.of(reservation));
            given(productStockRepository.findAllById(List.of(stockId)))
                    .willReturn(List.of(stock));

            List<ProductStockItem> items = List.of(new ProductStockItem(productId, orderItemId));

            productStockService.confirm(orderId, items);

            verify(productStateManager).soldOut(productId);
        }
    }
    @Nested
    @DisplayName("restore()")
    class Restore {

        @Test
        @DisplayName("이미 처리된 요청이면 아무 것도 하지 않고 반환한다 (멱등성)")
        void restore_alreadyProcessed_doesNothing() {
            ProductStockEventLog alreadyRestoredLog =
                    ProductStockEventLog.create(UUID.randomUUID(), orderItemId, StockEventType.RESTORE);
            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId), StockEventType.RESTORE))
                    .willReturn(List.of(alreadyRestoredLog));

            List<ProductStockItem> items = List.of(new ProductStockItem(productId, orderItemId));

            productStockService.restore(orderId, items);

            log.info("[ProductStockService.restore] orderItemId={} 이미 RESTORE 로그 존재 -> 예약 조회 없이 종료 기대", orderItemId);

            verify(productStockReservationRepository, never()).findAllById(any());
        }

        @Test
        @DisplayName("이미 처리된 항목은 건너뛰고, 나머지 항목은 정상 처리된다")
        void restore_alreadyProcessedItemSkipped_othersProcessed() {
            UUID productId2 = UUID.randomUUID();
            UUID orderItemId2 = UUID.randomUUID();
            UUID reservationId2 = UUID.randomUUID();
            UUID stockId2 = UUID.randomUUID();
            ProductStockReservation reservation2 = ProductStockReservation
                    .create(stockId2, orderId, 15, Instant.now().plusSeconds(1800));
            ReflectionTestUtils.setField(reservation2, "id", reservationId2);
            ProductStockEventLog reserveLog2 = ProductStockEventLog.create(reservationId2, orderItemId2, StockEventType.RESERVE);
            ProductStock stock2 = ProductStock.create(productId2, 50, 5);
            ReflectionTestUtils.setField(stock2, "id", stockId2);
            stock2.reserve(15);

            ProductStockEventLog alreadyRestoredLog =
                    ProductStockEventLog.create(UUID.randomUUID(), orderItemId, StockEventType.RESTORE);

            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId, orderItemId2), StockEventType.RESTORE))
                    .willReturn(List.of(alreadyRestoredLog)); // orderItemId만 이미 처리됨
            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId2), StockEventType.RESERVE))
                    .willReturn(List.of(reserveLog2));
            given(productStockReservationRepository.findAllById(List.of(reservationId2)))
                    .willReturn(List.of(reservation2));
            given(productStockRepository.findAllById(List.of(stockId2)))
                    .willReturn(List.of(stock2));

            List<ProductStockItem> items = List.of(
                    new ProductStockItem(productId, orderItemId),
                    new ProductStockItem(productId2, orderItemId2)
            );

            productStockService.restore(orderId, items);

            log.info("[ProductStockService.restore] orderItemId={}(이미 처리) 건너뜀, orderItemId2={} 정상 처리 기대",
                    orderItemId, orderItemId2);

            assertThat(reservation2.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
            ArgumentCaptor<Collection<ProductStock>> stockCaptor = ArgumentCaptor.forClass(Collection.class);
            verify(productStockRepository).saveAllAndFlush(stockCaptor.capture());
            assertThat(stockCaptor.getValue()).containsExactly(stock2);
        }

        @Test
        @DisplayName("정상 복구 시 예약 취소, 재고 복구, 이벤트로그 저장이 모두 실행된다")
        void restore_success() {
            UUID reservationId = UUID.randomUUID();
            UUID stockId = UUID.randomUUID();
            ProductStockReservation reservation = ProductStockReservation
                    .create(stockId, orderId, 20, Instant.now().plusSeconds(1800));
            ReflectionTestUtils.setField(reservation, "id", reservationId);
            ProductStockEventLog reserveLog = ProductStockEventLog.create(reservationId, orderItemId, StockEventType.RESERVE);
            ProductStock stock = ProductStock.create(productId, 100, 10);
            ReflectionTestUtils.setField(stock, "id", stockId);
            stock.reserve(20);

            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId), StockEventType.RESTORE))
                    .willReturn(List.of());
            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId), StockEventType.RESERVE))
                    .willReturn(List.of(reserveLog));
            given(productStockReservationRepository.findAllById(List.of(reservationId)))
                    .willReturn(List.of(reservation));
            given(productStockRepository.findAllById(List.of(stockId)))
                    .willReturn(List.of(stock));

            List<ProductStockItem> items = List.of(new ProductStockItem(productId, orderItemId));

            productStockService.restore(orderId, items);

            log.info("[ProductStockService.restore] orderItemId={} 복구 후 reservation.status={}, stock.available={}",
                    orderItemId, reservation.getStatus(), stock.getAvailableQuantity());

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
            verify(productStockReservationRepository).saveAllAndFlush(anyCollection());
            verify(productStockRepository).saveAllAndFlush(anyCollection());
            verify(productStockEventLogRepository).saveAllAndFlush(anyCollection());
        }

        @Test
        @DisplayName("예약에 연결된 상품과 요청 productId가 다르면 예외가 발생한다")
        void restore_ownershipMismatch_throwsException() {
            UUID reservationId = UUID.randomUUID();
            UUID stockId = UUID.randomUUID();
            UUID otherProductId = UUID.randomUUID();
            ProductStockReservation reservation = ProductStockReservation
                    .create(stockId, orderId, 20, Instant.now().plusSeconds(1800));
            ReflectionTestUtils.setField(reservation, "id", reservationId);
            ProductStockEventLog reserveLog = ProductStockEventLog.create(reservationId, orderItemId, StockEventType.RESERVE);
            ProductStock stock = ProductStock.create(otherProductId, 100, 10);
            ReflectionTestUtils.setField(stock, "id", stockId);

            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId), StockEventType.RESTORE))
                    .willReturn(List.of());
            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId), StockEventType.RESERVE))
                    .willReturn(List.of(reserveLog));
            given(productStockReservationRepository.findAllById(List.of(reservationId)))
                    .willReturn(List.of(reservation));
            given(productStockRepository.findAllById(List.of(stockId)))
                    .willReturn(List.of(stock));

            List<ProductStockItem> items = List.of(new ProductStockItem(productId, orderItemId));

            log.info("[ProductStockService.restore] 요청 productId={} != 재고 소유 productId={} -> RESERVATION_NOT_FOUND 예외 기대",
                    productId, otherProductId);

            assertThatThrownBy(() -> productStockService.restore(orderId, items))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_NOT_FOUND);
        }

        @Test
        @DisplayName("주문 ID가 예약과 일치하지 않으면 예외가 발생한다")
        void restore_orderIdMismatch_throwsException() {
            UUID reservationId = UUID.randomUUID();
            UUID stockId = UUID.randomUUID();
            UUID differentOrderId = UUID.randomUUID();
            ProductStockReservation reservation = ProductStockReservation
                    .create(stockId, differentOrderId, 20, Instant.now().plusSeconds(1800));
            ReflectionTestUtils.setField(reservation, "id", reservationId);
            ProductStockEventLog reserveLog = ProductStockEventLog.create(reservationId, orderItemId, StockEventType.RESERVE);

            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId), StockEventType.RESTORE))
                    .willReturn(List.of());
            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId), StockEventType.RESERVE))
                    .willReturn(List.of(reserveLog));
            given(productStockReservationRepository.findAllById(List.of(reservationId)))
                    .willReturn(List.of(reservation));

            List<ProductStockItem> items = List.of(new ProductStockItem(productId, orderItemId));

            log.info("[ProductStockService.restore] 요청 orderId={} != 예약된 orderId={} -> NOT_FOUND 예외 기대",
                    orderId, differentOrderId);

            assertThatThrownBy(() -> productStockService.restore(orderId, items))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_NOT_FOUND);
        }

        @Test
        @DisplayName("예약/재고 저장 시 낙관적 락 충돌이 발생하면 ALREADY_PROCESSED 에러로 변환된다")
        void restore_optimisticLockFailure_convertsToError() {
            UUID reservationId = UUID.randomUUID();
            UUID stockId = UUID.randomUUID();
            ProductStockReservation reservation = ProductStockReservation
                    .create(stockId, orderId, 20, Instant.now().plusSeconds(1800));
            ReflectionTestUtils.setField(reservation, "id", reservationId);
            ProductStockEventLog reserveLog = ProductStockEventLog.create(reservationId, orderItemId, StockEventType.RESERVE);
            ProductStock stock = ProductStock.create(productId, 100, 10);
            ReflectionTestUtils.setField(stock, "id", stockId);
            stock.reserve(20);

            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId), StockEventType.RESTORE))
                    .willReturn(List.of());
            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId), StockEventType.RESERVE))
                    .willReturn(List.of(reserveLog));
            given(productStockReservationRepository.findAllById(List.of(reservationId)))
                    .willReturn(List.of(reservation));
            given(productStockRepository.findAllById(List.of(stockId)))
                    .willReturn(List.of(stock));
            given(productStockReservationRepository.saveAllAndFlush(anyCollection()))
                    .willThrow(OptimisticLockingFailureException.class);

            List<ProductStockItem> items = List.of(new ProductStockItem(productId, orderItemId));

            log.info("[ProductStockService.restore] 예약 저장 시 낙관적 락 충돌 발생 -> ALREADY_PROCESSED 예외 기대");

            assertThatThrownBy(() -> productStockService.restore(orderId, items))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);
        }

        @Test
        @DisplayName("예약이 이미 EXPIRED 상태면 재고/로그를 재처리하지 않고 반환한다")
        void restore_reservationExpired_doesNothingSilently() {
            UUID reservationId = UUID.randomUUID();
            UUID stockId = UUID.randomUUID();
            ProductStockReservation reservation = ProductStockReservation
                    .create(stockId, orderId, 20, Instant.now().plusSeconds(1800));
            ReflectionTestUtils.setField(reservation, "id", reservationId);
            reservation.expire();
            ProductStockEventLog reserveLog = ProductStockEventLog.create(reservationId, orderItemId, StockEventType.RESERVE);

            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId), StockEventType.RESTORE))
                    .willReturn(List.of());
            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId), StockEventType.RESERVE))
                    .willReturn(List.of(reserveLog));
            given(productStockReservationRepository.findAllById(List.of(reservationId)))
                    .willReturn(List.of(reservation));

            List<ProductStockItem> items = List.of(new ProductStockItem(productId, orderItemId));

            log.info("[ProductStockService.restore] 예약 상태=EXPIRED -> 재고/로그 재처리 없이 반환 기대");

            productStockService.restore(orderId, items);

            verify(productStockRepository, never()).findAllById(any());
            verify(productStockEventLogRepository, never()).saveAllAndFlush(any());
        }

        @Test
        @DisplayName("예약이 EXPIRATION_FAILED 상태면 격리 에러로 응답한다")
        void restore_reservationIsolated_throwsIsolatedError() {
            UUID reservationId = UUID.randomUUID();
            UUID stockId = UUID.randomUUID();
            ProductStockReservation reservation = ProductStockReservation
                    .create(stockId, orderId, 20, Instant.now().plusSeconds(1800));
            ReflectionTestUtils.setField(reservation, "id", reservationId);
            reservation.fail();
            ProductStockEventLog reserveLog = ProductStockEventLog.create(reservationId, orderItemId, StockEventType.RESERVE);

            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId), StockEventType.RESTORE))
                    .willReturn(List.of());
            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId), StockEventType.RESERVE))
                    .willReturn(List.of(reserveLog));
            given(productStockReservationRepository.findAllById(List.of(reservationId)))
                    .willReturn(List.of(reservation));

            List<ProductStockItem> items = List.of(new ProductStockItem(productId, orderItemId));

            log.info("[ProductStockService.restore] 예약 상태=EXPIRATION_FAILED -> ISOLATED 예외 기대");

            assertThatThrownBy(() -> productStockService.restore(orderId, items))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_ISOLATED);
        }

        @Test
        @DisplayName("두 번째 항목이 격리 상태면 예외가 전파되고 재고 조회는 실행되지 않는다")
        void restore_bulkFailure_stopsProcessingRemainingItems() {
            UUID productId2 = UUID.randomUUID();
            UUID productId3 = UUID.randomUUID();
            UUID orderItemId2 = UUID.randomUUID();
            UUID orderItemId3 = UUID.randomUUID();

            UUID reservationId1 = UUID.randomUUID();
            ProductStockReservation reservation1 = ProductStockReservation
                    .create(UUID.randomUUID(), orderId, 10, Instant.now().plusSeconds(1800));
            ReflectionTestUtils.setField(reservation1, "id", reservationId1);
            ProductStockEventLog reserveLog1 = ProductStockEventLog.create(reservationId1, orderItemId, StockEventType.RESERVE);

            UUID reservationId2 = UUID.randomUUID();
            ProductStockReservation reservation2 = ProductStockReservation
                    .create(UUID.randomUUID(), orderId, 10, Instant.now().plusSeconds(1800));
            ReflectionTestUtils.setField(reservation2, "id", reservationId2);
            reservation2.fail(); // 격리 상태 유발
            ProductStockEventLog reserveLog2 = ProductStockEventLog.create(reservationId2, orderItemId2, StockEventType.RESERVE);

            UUID reservationId3 = UUID.randomUUID();
            ProductStockReservation reservation3 = ProductStockReservation
                    .create(UUID.randomUUID(), orderId, 10, Instant.now().plusSeconds(1800));
            ReflectionTestUtils.setField(reservation3, "id", reservationId3);
            ProductStockEventLog reserveLog3 = ProductStockEventLog.create(reservationId3, orderItemId3, StockEventType.RESERVE);

            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId, orderItemId2, orderItemId3), StockEventType.RESTORE))
                    .willReturn(List.of());
            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId, orderItemId2, orderItemId3), StockEventType.RESERVE))
                    .willReturn(List.of(reserveLog1, reserveLog2, reserveLog3));
            // reservationIds는 HashMap(reservationIdByOrderItemId)에서 만들어져 순서가 보장되지 않으므로
            // 특정 순서의 List로 스텁하지 않고 any()로 느슨하게 받는다
            given(productStockReservationRepository.findAllById(any()))
                    .willReturn(List.of(reservation1, reservation2, reservation3));

            List<ProductStockItem> items = List.of(
                    new ProductStockItem(productId, orderItemId),
                    new ProductStockItem(productId2, orderItemId2),
                    new ProductStockItem(productId3, orderItemId3)
            );

            log.info("[ProductStockService.restore] 두 번째 항목 격리 상태 -> 예외 전파, 재고 조회 미실행 기대");

            assertThatThrownBy(() -> productStockService.restore(orderId, items))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_RESERVATION_ISOLATED);

            // 격리 상태에서 예외가 터지므로 재고 조회 자체가 실행되지 않아야 함
            verify(productStockRepository, never()).findAllById(any());
        }
    }


    @Nested
    @DisplayName("이벤트로그 저장 멱등성")
    class EventLogIdempotency {

        @Test
        @DisplayName("동시 요청으로 UNIQUE 제약이 걸리면 ALREADY_PROCESSED 에러로 변환된다")
        void saveEventLog_uniqueViolation_convertsToAlreadyProcessed() {
            ProductStock stock = ProductStock.create(productId, 100, 10);
            given(productStockEventLogRepository.findByOrderItemIdInAndEventType(
                    List.of(orderItemId), StockEventType.RESERVE))
                    .willReturn(List.of());
            given(productStockRepository.findByProductIdInAndDeletedAtIsNull(List.of(productId)))
                    .willReturn(List.of(stock));
            given(productStockEventLogRepository.saveAllAndFlush(anyCollection()))
                    .willThrow(DataIntegrityViolationException.class);

            List<ProductStockReserveItem> items = List.of(
                    new ProductStockReserveItem(productId, orderItemId, 10)
            );

            log.info("[ProductStockService.reserve] 이벤트로그 저장 시 UNIQUE 제약 위반(동시 요청) -> ALREADY_PROCESSED 예외 기대");

            assertThatThrownBy(() -> productStockService.reserve(orderId, items))
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
            doThrow(new BusinessException(ErrorCode.PRODUCT_STOCK_FORBIDDEN))
                    .when(authorizationChecker).requireOwnerOrAdmin(any(), any(), any());
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

    @Nested
    @DisplayName("deallocate()")
    class Deallocate {

        @Test
        @DisplayName("판매자 본인이 요청하면 정상 반환된다")
        void deallocate_bySeller_success() {
            ProductStock stock = ProductStock.create(productId, 100, 10);
            stock.allocate(30); // total=70, available=70로 미리 차감해둠

            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(stock));
            given(productReader.getSellerId(productId)).willReturn(sellerId);
            ProductStockAllocateCommand command = new ProductStockAllocateCommand(productId, 30, sellerId, "SELLER");
            productStockService.deallocate(command);

            log.info("[ProductStockService.deallocate] 30개 반환 후 total={}, available={}",
                    stock.getTotalQuantity(), stock.getAvailableQuantity());

            assertThat(stock.getTotalQuantity()).isEqualTo(100);
            assertThat(stock.getAvailableQuantity()).isEqualTo(100);
        }

        @Test
        @DisplayName("관리자가 요청하면 소유자가 아니어도 반환된다")
        void deallocate_byAdmin_success() {
            ProductStock stock = ProductStock.create(productId, 100, 10);
            stock.allocate(30);

            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(stock));
            given(productReader.getSellerId(productId)).willReturn(sellerId);
            UUID adminId = UUID.randomUUID();
            ProductStockAllocateCommand command = new ProductStockAllocateCommand(productId, 30, adminId, "ADMIN");

            productStockService.deallocate(command);

            log.info("[ProductStockService.deallocate] 관리자 요청 -> 소유자 아니어도 반환 기대, total={}", stock.getTotalQuantity());

            assertThat(stock.getTotalQuantity()).isEqualTo(100);
        }

        @Test
        @DisplayName("소유자가 아닌 판매자가 요청하면 예외가 발생")
        void deallocate_notOwner_throwsForbidden() {
            given(productReader.getSellerId(productId)).willReturn(sellerId);
            doThrow(new BusinessException(ErrorCode.PRODUCT_STOCK_FORBIDDEN))
                    .when(authorizationChecker).requireOwnerOrAdmin(any(), any(), any());
            UUID otherSellerId = UUID.randomUUID();
            ProductStockAllocateCommand command = new ProductStockAllocateCommand(productId, 30, otherSellerId, "SELLER");

            log.info("[ProductStockService.deallocate] 소유자 아닌 판매자 요청 -> FORBIDDEN 예외 기대");

            assertThatThrownBy(() -> productStockService.deallocate(command))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_FORBIDDEN);
        }

        @Test
        @DisplayName("재고가 없으면 예외가 발생")
        void deallocate_stockNotFound_throwsException() {

            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId)).willReturn(Optional.empty());
            given(productReader.getSellerId(productId)).willReturn(sellerId);
            ProductStockAllocateCommand command = new ProductStockAllocateCommand(productId, 30, sellerId, "SELLER");

            log.info("[ProductStockService.deallocate] 재고 없음 -> NOT_FOUND 예외 기대");

            assertThatThrownBy(() -> productStockService.deallocate(command))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_NOT_FOUND);
        }

        @Test
        @DisplayName("낙관적 락 충돌 시 CONFLICT 에러로 변환된다")
        void deallocate_optimisticLockFailure_throwsConflict() {
            ProductStock stock = ProductStock.create(productId, 100, 10);
            stock.allocate(30);

            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(stock));
            given(productReader.getSellerId(productId)).willReturn(sellerId);
            given(productStockRepository.saveAndFlush(any(ProductStock.class))).willThrow(OptimisticLockingFailureException.class);

            ProductStockAllocateCommand command = new ProductStockAllocateCommand(productId, 30, sellerId, "SELLER");

            log.info("[ProductStockService.deallocate] 저장 시 낙관적 락 충돌 -> CONFLICT 예외 기대");

            assertThatThrownBy(() -> productStockService.deallocate(command))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_CONFLICT);
        }

        @Test
        @DisplayName("반환으로 재고가 0에서 벗어나면 재입고 알림이 호출")
        void deallocate_fromZero_notifiesRestocked() {
            ProductStock stock = ProductStock.create(productId, 30, 5);
            stock.allocate(30); // 미리 전량 소진시켜 SOLD_OUT 상태로 만들어둠

            given(productStockRepository.findByProductIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(stock));
            given(productReader.getSellerId(productId)).willReturn(sellerId);

            ProductStockAllocateCommand command = new ProductStockAllocateCommand(productId, 10, sellerId, "SELLER");

            productStockService.deallocate(command);

            verify(productStateManager).resumeSaleAfterRestock(productId);
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
