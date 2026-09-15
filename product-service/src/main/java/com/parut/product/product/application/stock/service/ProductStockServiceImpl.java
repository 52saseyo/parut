package com.parut.product.product.application.stock.service;


import com.parut.product.global.dto.ProductStockAllocateCommand;
import com.parut.product.global.dto.ProductStockAllocateResult;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.product.application.authorization.stock.ProductStockAuthorizationChecker;
import com.parut.product.product.application.stock.dto.ProductStockItem;
import com.parut.product.product.application.stock.dto.ProductStockReserveItem;
import com.parut.product.product.application.product.manager.ProductStateManager;
import com.parut.product.product.application.product.reader.ProductReader;
import com.parut.product.product.application.stock.dto.IsolatedReservationResult;
import com.parut.product.product.domain.product.Product;
import com.parut.product.product.domain.product.ProductStatus;
import com.parut.product.product.domain.stock.entity.ProductStock;
import com.parut.product.product.domain.stock.entity.ProductStockEventLog;
import com.parut.product.product.domain.stock.entity.ProductStockReservation;
import com.parut.product.product.domain.stock.enums.ReservationStatus;
import com.parut.product.product.domain.stock.enums.StockEventType;
import com.parut.product.product.domain.stock.enums.StockStatus;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockEventLogRepository;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockRepository;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductStockServiceImpl implements ProductStockService{


    @Value("${parut.product-stock.reservation-ttl}")
    private Duration reservationTtl;


    private final ProductStockRepository productStockRepository;
    private final ProductStockReservationRepository productStockReservationRepository;
    private final ProductStockEventLogRepository productStockEventLogRepository;
    private final ProductReader productReader;
    private final ProductStateManager productStateManager;
    private final ProductStockAuthorizationChecker authorizationChecker;

    // 상품 등록 시 재고 등록
    @Override
    public void createStock(UUID productId, int totalQuantity, int lowStockThreshold) {
        ProductStock stock = ProductStock.create(productId, totalQuantity, lowStockThreshold);

        try {
            productStockRepository.save(stock);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.PRODUCT_STOCK_ALREADY_EXISTS);
        }
    }

    // 상품 한 개의 재고 조회
    @Override
    @Transactional(readOnly = true)
    public ProductStock getStock(UUID productId) {
        return productStockRepository.findByProductIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_FOUND));
    }

    // 상품 여러 개 재고 조회
    @Override
    @Transactional(readOnly = true)
    public List<ProductStock> getStocks(List<UUID> productIds) {
        return productStockRepository.findByProductIdInAndDeletedAtIsNull(productIds);
    }

    // 재고 수정
    @Override
    public void updateStock(UUID productId, UUID requesterId, String requesterRole, int newTotalQuantity) {
        ProductStock stock = productStockRepository.findByProductIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_FOUND));

        UUID sellerId = productReader.getSellerId(productId);
        authorizationChecker.requireOwnerOrAdmin(requesterId, requesterRole, sellerId);

        StockStatus previousStatus = stock.getStatus();

        int reservedQuantity = stock.getTotalQuantity() - stock.getAvailableQuantity();
        if (newTotalQuantity < reservedQuantity) {
            throw new BusinessException(ErrorCode.PRODUCT_STOCK_INVALID_QUANTITY);
        }

        int newAvailableQuantity = newTotalQuantity - reservedQuantity;
        stock.adjustQuantity(newTotalQuantity, newAvailableQuantity);
        saveStockSafely(stock, ErrorCode.PRODUCT_STOCK_CONFLICT);

        if (stock.getStatus() == StockStatus.SOLD_OUT) {
            notifySoldOut(productId);
        } else if (previousStatus == StockStatus.SOLD_OUT) {
            notifyRestocked(productId);
        }

    }

    // 재고 삭제
    @Override
    public void deleteStock(UUID productId, String deletedBy) {
        ProductStock stock = productStockRepository.findByProductIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_FOUND));
        stock.softDelete(deletedBy);
        saveStockSafely(stock, ErrorCode.PRODUCT_STOCK_CONFLICT);
    }

    @Override
    public void reserve(UUID orderId, List<ProductStockReserveItem> items) {
        Instant expiresAt = Instant.now().plus(reservationTtl);
        // 1. 멱등성 체크 - IN절 한 번
        List<ProductStockReserveItem> targetItems =
                filterUnprocessed(items, ProductStockReserveItem::orderItemId, StockEventType.RESERVE);
        if (targetItems.isEmpty()) {
            return; // 전부 이미 처리됨 - 멱등 반환
        }
        // 2. 재고 조회 - IN절 한 번
        List<UUID> productIds = targetItems.stream().map(ProductStockReserveItem::productId).distinct().toList();
        Map<UUID, ProductStock> stockByProductId = productStockRepository
                .findByProductIdInAndDeletedAtIsNull(productIds)
                .stream()
                .collect(Collectors.toMap(ProductStock::getProductId, s -> s));

        // 3. 도메인 로직 적용 (메모리, DB 호출 없음) - reservation까지만 먼저 만든다
        List<ProductStockReservation> reservationsToSave = new ArrayList<>();

        for (ProductStockReserveItem item : targetItems) {
            ProductStock stock = stockByProductId.get(item.productId());
            if (stock == null) {
                log.warn("[reserve] 재고 없음 - orderId={}, orderItemId={}, productId={}",
                        orderId, item.orderItemId(), item.productId());
                throw new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_FOUND);
            }
            stock.reserve(item.quantity());
            reservationsToSave.add(ProductStockReservation.create(stock.getId(), orderId, item.quantity(), expiresAt));
        }

        // 4. 저장 - 재고와 예약 먼저 (예약은 flush까지 해야 id가 채워짐)
        try {
            productStockRepository.saveAllAndFlush(stockByProductId.values());
            productStockReservationRepository.saveAllAndFlush(reservationsToSave);
        } catch (OptimisticLockingFailureException e) {
            log.warn("[reserve] 낙관적 락 충돌 - orderId={}", orderId);
            throw new BusinessException(ErrorCode.PRODUCT_STOCK_CONFLICT);
        }

        // 5. reservation.getId()가 채워졌으니, targetItems와 같은 순서로 짝지어 eventLog 생성
        List<ProductStockEventLog> eventLogsToSave = new ArrayList<>();
        for (int i = 0; i < targetItems.size(); i++) {
            eventLogsToSave.add(ProductStockEventLog.create(
                    reservationsToSave.get(i).getId(),
                    targetItems.get(i).orderItemId(),
                    StockEventType.RESERVE
            ));
        }
        // 6. 이벤트로그 저장 - 유니크 제약 위반만 별도로 좁게 catch
        saveEventLogsSafely(eventLogsToSave, "reserve", orderId);
    }
    @Override
    public void confirm(UUID orderId, List<ProductStockItem> items) {
        // 1. 멱등성 체크 - IN절 한 번
        List<ProductStockItem> targetItems =
                filterUnprocessed(items, ProductStockItem::orderItemId, StockEventType.CONFIRM);
        if (targetItems.isEmpty()) {
            return;
        }

        // 2. RESERVE 로그 조회 + 예약 조회 - IN절 두 번 (존재 검증 포함, loadValidatedReservations)
        List<UUID> targetOrderItemIds = targetItems.stream().map(ProductStockItem::orderItemId).toList();
        Map<UUID, ProductStockReservation> reservationByOrderItemId = loadValidatedReservations(targetOrderItemIds);

        // 3. reservation 검증(null/orderId 불일치) + confirm() 도메인 로직 - stockIds 계산보다 먼저 해야 안전함
        for (ProductStockItem item : targetItems) {
            ProductStockReservation reservation = reservationByOrderItemId.get(item.orderItemId());
            if (reservation == null || !reservation.getOrderId().equals(orderId)) {
                throw new BusinessException(ErrorCode.PRODUCT_STOCK_RESERVATION_NOT_FOUND);
            }
            reservation.confirm();
        }

        // 4. 재고 조회 - IN절 한 번 (3번에서 reservation 검증이 끝난 뒤라 안전하게 조회 가능)
        List<UUID> stockIds = targetItems.stream()
                .map(item -> reservationByOrderItemId.get(item.orderItemId()).getStockId())
                .distinct()
                .toList();
        Map<UUID, ProductStock> stockById = productStockRepository
                .findAllById(stockIds)
                .stream()
                .collect(Collectors.toMap(ProductStock::getId, s -> s));

        // 5. stock 검증 + 도메인 로직 + 이벤트로그 준비
        List<ProductStockEventLog> eventLogsToSave = new ArrayList<>();
        Set<UUID> soldOutProductIds = new HashSet<>();

        for (ProductStockItem item : targetItems) {
            ProductStockReservation reservation = reservationByOrderItemId.get(item.orderItemId());
            ProductStock stock = stockById.get(reservation.getStockId());
            if (stock == null) {
                throw new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_FOUND);
            }
            if (!stock.getProductId().equals(item.productId())) {
                throw new BusinessException(ErrorCode.PRODUCT_STOCK_RESERVATION_NOT_FOUND);
            }
            stock.confirm(reservation.getQuantity());
            if (stock.getStatus() == StockStatus.SOLD_OUT) {
                soldOutProductIds.add(item.productId());
            }
            eventLogsToSave.add(ProductStockEventLog.create(reservation.getId(), item.orderItemId(), StockEventType.CONFIRM));
        }

        // 6. 저장 - 배치로 한 번에
        try {
            // orderItemId : reservation = 1:1 (reserve()가 orderItemId당 예약을 하나씩만 만듦)
            productStockReservationRepository.saveAllAndFlush(reservationByOrderItemId.values());
            productStockRepository.saveAllAndFlush(stockById.values());
        } catch (OptimisticLockingFailureException e) {
            log.warn("[confirm] 낙관적 락 충돌 - orderId={}", orderId);
            throw new BusinessException(ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);
        }

        saveEventLogsSafely(eventLogsToSave, "confirm", orderId);

        // 7. 품절 알림 - 배치 처리 후 한 번에
        soldOutProductIds.forEach(this::notifySoldOut);
    }

    @Override
    public void restore(UUID orderId, List<ProductStockItem> items) {
        // 1. 멱등성 체크 - IN절 한 번
        List<ProductStockItem> targetItems =
                filterUnprocessed(items, ProductStockItem::orderItemId, StockEventType.RESTORE);

        if (targetItems.isEmpty()) {
            return;
        }

        // 2. RESERVE 로그 조회 + 예약 조회 - IN절 두 번 (존재 검증 포함, loadValidatedReservations)
        List<UUID> targetOrderItemIds = targetItems.stream().map(ProductStockItem::orderItemId).toList();
        Map<UUID, ProductStockReservation> reservationByOrderItemId = loadValidatedReservations(targetOrderItemIds);

        // 3. reservation 검증(null/orderId 불일치) + 상태별 분기
        //    EXPIRED -> 이미 스케줄러가 처리 완료, 멱등 스킵 / EXPIRATION_FAILED -> 격리 상태, 즉시 예외
        //    나머지(RESERVED)만 실제 복구 대상(actuallyRestoreItems)으로 확정
        List<ProductStockItem> actuallyRestoreItems = new ArrayList<>();
        for (ProductStockItem item : targetItems) {
            ProductStockReservation reservation = reservationByOrderItemId.get(item.orderItemId());
            if (reservation == null || !reservation.getOrderId().equals(orderId)) {
                throw new BusinessException(ErrorCode.PRODUCT_STOCK_RESERVATION_NOT_FOUND);
            }

            if (reservation.getStatus() == ReservationStatus.EXPIRED) {
                continue;
            }
            if (reservation.getStatus() == ReservationStatus.EXPIRATION_FAILED) {
                log.warn("[restore] 격리된 예약 - orderId={}, orderItemId={}, reservationId={}",
                        orderId, item.orderItemId(), reservation.getId());
                throw new BusinessException(ErrorCode.PRODUCT_STOCK_RESERVATION_ISOLATED);
            }
            actuallyRestoreItems.add(item);
        }

        if (actuallyRestoreItems.isEmpty()) {
            return;
        }

        // 4. 재고 조회 - IN절 한 번 (3번에서 reservation 검증/분류가 끝난 뒤라 안전하게 조회 가능)
        List<UUID> stockIds = actuallyRestoreItems.stream()
                .map(item -> reservationByOrderItemId.get(item.orderItemId()).getStockId())
                .distinct()
                .toList();
        Map<UUID, ProductStock> stockById = productStockRepository
                .findAllById(stockIds)
                .stream()
                .collect(Collectors.toMap(ProductStock::getId, s -> s));

        // 5. stock 검증 + 도메인 로직 + 이벤트로그 준비
        List<ProductStockEventLog> eventLogsToSave = new ArrayList<>();
        List<ProductStockReservation> reservationsToSave = new ArrayList<>();
        for (ProductStockItem item : actuallyRestoreItems) {
            ProductStockReservation reservation = reservationByOrderItemId.get(item.orderItemId());
            reservation.cancel();
            reservationsToSave.add(reservation);

            ProductStock stock = stockById.get(reservation.getStockId());
            if (stock == null || !stock.getProductId().equals(item.productId())) {
                throw new BusinessException(ErrorCode.PRODUCT_STOCK_RESERVATION_NOT_FOUND);
            }
            stock.restore(reservation.getQuantity());

            eventLogsToSave.add(ProductStockEventLog.create(reservation.getId(), item.orderItemId(), StockEventType.RESTORE));
        }

        // 6. 저장 - 배치로 한 번에
        try {
            productStockReservationRepository.saveAllAndFlush(reservationsToSave);
            productStockRepository.saveAllAndFlush(stockById.values());
        } catch (OptimisticLockingFailureException e) {
            log.warn("[restore] 낙관적 락 충돌 - orderId={}", orderId);
            throw new BusinessException(ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);
        }

        saveEventLogsSafely(eventLogsToSave, "restore", orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductStock> getStockList(UUID requesterId, String requesterRole, Pageable pageable) {
        if (authorizationChecker.isAdmin(requesterRole)) {
            return productStockRepository.findByDeletedAtIsNull(pageable);
        }
        List<UUID> productIds = productReader.getProductIdsBySellerId(requesterId);
        return productStockRepository.findByProductIdInAndDeletedAtIsNull(productIds, pageable);
    }

    // 타임딜 전환 메서드
    @Override
    public ProductStockAllocateResult allocate(ProductStockAllocateCommand command) {
        Product product = productReader.getProduct(command.productId());
        authorizationChecker.requireOwnerOrAdmin(command.requesterId(), command.requesterRole(), product.getSellerId());

        if (product.getStatus() != ProductStatus.ON_SALE) {
            throw new BusinessException(ErrorCode.PRODUCT_STOCK_PRODUCT_NOT_ON_SALE);
        }

        ProductStock stock = productStockRepository.findByProductIdAndDeletedAtIsNull(command.productId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_FOUND));

        stock.allocate(command.quantity());
        saveStockSafely(stock, ErrorCode.PRODUCT_STOCK_CONFLICT);
        if (stock.getStatus() == StockStatus.SOLD_OUT) {
            notifySoldOut(command.productId());
        }
        return new ProductStockAllocateResult(
                product.getId(),
                product.getSellerId(),
                product.getImageId(),
                command.quantity(),
                product.getName(),
                product.getDescription(),
                product.getAppearanceType().name(),
                product.getOrigin(),
                product.getHarvestDate(),
                product.getPrice()
        );
    }

    // 재고 복구 메서드
    @Override
    public void deallocate(ProductStockAllocateCommand command) {
        UUID sellerId = productReader.getSellerId(command.productId());
        authorizationChecker.requireOwnerOrAdmin(command.requesterId(), command.requesterRole(), sellerId);

        ProductStock stock = productStockRepository.findByProductIdAndDeletedAtIsNull(command.productId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_FOUND));

        StockStatus previousStatus = stock.getStatus();

        stock.deallocate(command.quantity());
        saveStockSafely(stock, ErrorCode.PRODUCT_STOCK_CONFLICT);
        if (previousStatus == StockStatus.SOLD_OUT && stock.getStatus() != StockStatus.SOLD_OUT) {
            notifyRestocked(command.productId());
        }
    }

    // 낙관적 락 검증 (재고)
    private void saveStockSafely(ProductStock stock, ErrorCode conflictErrorCode) {
        try {
            productStockRepository.saveAndFlush(stock);
        } catch (OptimisticLockingFailureException e) {
            throw new BusinessException(conflictErrorCode);
        }
    }

    // 품절 알림 메서드
    private void notifySoldOut(UUID productId) {
        try {
            productStateManager.soldOut(productId);
        } catch (BusinessException e) {
            if (e.getErrorCode() != ErrorCode.PRODUCT_STATUS_TRANSITION_NOT_ALLOWED) {
                throw e;
            }
            // 이미 SOLD_OUT 상태라 전이 불가 -> 멱등 처리, 무시
        }
    }

    private void notifyRestocked(UUID productId) {
        try {
            productStateManager.resumeSaleAfterRestock(productId);
        } catch (BusinessException e) {
            if (e.getErrorCode() != ErrorCode.PRODUCT_STATUS_TRANSITION_NOT_ALLOWED
                    && e.getErrorCode() != ErrorCode.PRODUCT_IMAGE_REQUIRED) {
                throw e;
            }
        }
    }

    // 멱등성 체크 - 이미 처리된 orderItemId 제외 (IN절 한 번)
    private <T> List<T> filterUnprocessed(
            List<T> items,
            Function<T, UUID> orderItemIdExtractor,
            StockEventType eventType
    ) {
        List<UUID> orderItemIds = items.stream().map(orderItemIdExtractor).toList();
        Set<UUID> alreadyProcessed = productStockEventLogRepository
                .findByOrderItemIdInAndEventType(orderItemIds, eventType)
                .stream()
                .map(ProductStockEventLog::getOrderItemId)
                .collect(Collectors.toSet());

        return items.stream()
                .filter(item -> !alreadyProcessed.contains(orderItemIdExtractor.apply(item)))
                .toList();
    }

    // RESERVE 로그 조회 + 예약 조회 - 존재 검증 포함 (IN절 두 번)
    private Map<UUID, ProductStockReservation> loadValidatedReservations(List<UUID> targetOrderItemIds) {
        List<ProductStockEventLog> reserveLogs = productStockEventLogRepository
                .findByOrderItemIdInAndEventType(targetOrderItemIds, StockEventType.RESERVE);

        for (UUID orderItemId : targetOrderItemIds) {
            boolean found = reserveLogs.stream().anyMatch(l -> l.getOrderItemId().equals(orderItemId));
            if (!found) {
                throw new BusinessException(ErrorCode.PRODUCT_STOCK_RESERVATION_NOT_FOUND);
            }
        }

        Map<UUID, UUID> reservationIdByOrderItemId = reserveLogs.stream()
                .collect(Collectors.toMap(ProductStockEventLog::getOrderItemId, ProductStockEventLog::getReservationId));

        // HashMap.values()는 순서를 보장하지 않으므로, 순서가 보장된 targetOrderItemIds 기준으로 만들어 IN절 인자 순서를 결정적으로 유지
        List<UUID> reservationIds = targetOrderItemIds.stream()
                .map(reservationIdByOrderItemId::get)
                .distinct()
                .toList();
        Map<UUID, ProductStockReservation> reservationById = productStockReservationRepository
                .findAllById(reservationIds)
                .stream()
                .collect(Collectors.toMap(ProductStockReservation::getId, r -> r));

        Map<UUID, ProductStockReservation> reservationByOrderItemId = new HashMap<>();
        reservationIdByOrderItemId.forEach((orderItemId, reservationId) ->
                reservationByOrderItemId.put(orderItemId, reservationById.get(reservationId)));
        return reservationByOrderItemId;
    }

    // 이벤트로그 저장 - 유니크 제약 위반만 별도로 좁게 catch
    private void saveEventLogsSafely(List<ProductStockEventLog> eventLogs, String actionName, UUID orderId) {
        try {
            productStockEventLogRepository.saveAllAndFlush(eventLogs);
        } catch (DataIntegrityViolationException e) {
            log.warn("[{}] 이벤트로그 유니크 제약 위반 (동시 요청) - orderId={}", actionName, orderId);
            throw new BusinessException(ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);
        }
    }
}
