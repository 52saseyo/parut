package com.parut.product.product.application.stock.service;


import com.parut.product.global.common.UserRole;
import com.parut.product.global.dto.ProductStockAllocateCommand;
import com.parut.product.global.dto.ProductStockAllocateResult;
import com.parut.product.global.dto.ProductStockTransferCommand;
import com.parut.product.global.dto.ProductStockTransferResult;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.product.application.authorization.stock.ProductStockAuthorizationChecker;
import com.parut.product.product.application.stock.dto.*;
import com.parut.product.product.application.product.manager.ProductStateManager;
import com.parut.product.product.application.product.reader.ProductReader;
import com.parut.product.product.application.product.port.out.ProductImagePort;
import com.parut.product.product.application.product.port.out.dto.ProductImageResult;
import com.parut.product.product.domain.product.Product;
import com.parut.product.product.domain.product.ProductStatus;
import com.parut.product.product.domain.stock.entity.ProductStock;
import com.parut.product.product.domain.stock.entity.ProductStockAllocationLog;
import com.parut.product.product.domain.stock.entity.ProductStockEventLog;
import com.parut.product.product.domain.stock.entity.ProductStockReservation;
import com.parut.product.product.domain.stock.enums.AllocationEventType;
import com.parut.product.product.domain.stock.enums.ReservationStatus;
import com.parut.product.product.domain.stock.enums.StockEventType;
import com.parut.product.product.domain.stock.enums.StockStatus;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockAllocationLogRepository;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockEventLogRepository;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockRepository;
import com.parut.product.product.infrastructure.stock.persistence.ProductStockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final ProductStockAllocationLogRepository productStockAllocationLogRepository;
    private final ProductImagePort productImagePort;

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

    // 재고 이력 조회
    @Override
    @Transactional(readOnly = true)
    public ProductStockHistoryResult getStockHistory(UUID productId, UUID requesterId, String requesterRole, String cursor, int size) {

        HistoryCursor parsedCursor = HistoryCursor.decode(cursor);
        Instant eventLogCursorCreatedAt = parsedCursor.eventLogCreatedAt();
        UUID eventLogCursorId = parsedCursor.eventLogId();
        Instant allocationCursorCreatedAt = parsedCursor.allocationCreatedAt();
        UUID allocationCursorId = parsedCursor.allocationId();

        UUID sellerId = productReader.getSellerId(productId);
        authorizationChecker.requireOwnerOrAdmin(requesterId, requesterRole, sellerId);
        ProductStock stock = productStockRepository.findByProductIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_FOUND));

        Pageable pageable = PageRequest.of(0, size);

        // RESERVE/CONFIRM/RESTORE: ProductStockReservation과 조인해서 stockId로 바로 거름(size만큼만 조회, 예약 전체 조회 없음)
        List<ProductStockEventLog> eventLogs = (eventLogCursorCreatedAt == null)
                ? productStockEventLogRepository.findFirstHistoryBatch(stock.getId(), pageable)
                : productStockEventLogRepository.findNextHistoryBatchByCursor(
                stock.getId(), eventLogCursorCreatedAt, eventLogCursorId, pageable);

        // 위에서 뽑힌 이벤트로그가 참조하는 예약만(최대 size개) 조회해서 quantity 매핑
        List<UUID> pageReservationIds = eventLogs.stream()
                .map(ProductStockEventLog::getReservationId)
                .distinct()
                .toList();
        Map<UUID, Integer> quantityByReservationId = productStockReservationRepository.findAllById(pageReservationIds)
                .stream()
                .collect(Collectors.toMap(ProductStockReservation::getId, ProductStockReservation::getQuantity));

        List<ProductStockAllocationLog> allocationLogs = (allocationCursorCreatedAt == null)
                ? productStockAllocationLogRepository.findFirstHistoryBatch(List.of(stock.getId()), pageable)
                : productStockAllocationLogRepository.findNextHistoryBatchByCursor(
                List.of(stock.getId()), allocationCursorCreatedAt, allocationCursorId, pageable);

        List<ProductStockHistoryItem> items = Stream.concat(
                        eventLogs.stream().map(log -> new ProductStockHistoryItem(
                                log.getId(), "EVENT_LOG", log.getEventType().name(),
                                quantityByReservationId.get(log.getReservationId()),
                                log.getCreatedAt(), log.getCreatedBy())),
                        allocationLogs.stream().map(log -> new ProductStockHistoryItem(
                                log.getId(), "ALLOCATION_LOG", log.getEventType().name(), log.getQuantity(),
                                log.getCreatedAt(), log.getCreatedBy()))
                )
                .sorted(Comparator.comparing(ProductStockHistoryItem::occurredAt).reversed())
                .limit(size)
                .toList();

        // 핵심: 이번 페이지에 실제로 "포함된" 항목 중, 그 소스의 마지막 것으로만 다음 커서를 잡는다.
        // 그 소스에서 하나도 안 뽑혔으면 커서를 그대로 유지(다음 페이지에서 같은 지점부터 다시 시도).
        ProductStockHistoryItem lastEventLogItem = items.stream()
                .filter(i -> i.source().equals("EVENT_LOG"))
                .reduce((first, second) -> second).orElse(null);
        ProductStockHistoryItem lastAllocationItem = items.stream()
                .filter(i -> i.source().equals("ALLOCATION_LOG"))
                .reduce((first, second) -> second).orElse(null);

        Instant nextEventLogCursorCreatedAt = lastEventLogItem != null ? lastEventLogItem.occurredAt() : eventLogCursorCreatedAt;
        UUID nextEventLogCursorId = lastEventLogItem != null ? lastEventLogItem.id() : eventLogCursorId;
        Instant nextAllocationCursorCreatedAt = lastAllocationItem != null ? lastAllocationItem.occurredAt() : allocationCursorCreatedAt;
        UUID nextAllocationCursorId = lastAllocationItem != null ? lastAllocationItem.id() : allocationCursorId;

        Product product = productReader.getProduct(productId);
        String nextCursor = new HistoryCursor(
                nextEventLogCursorCreatedAt, nextEventLogCursorId,
                nextAllocationCursorCreatedAt, nextAllocationCursorId
        ).encode();
        return new ProductStockHistoryResult(product.getName(), items, nextCursor);
    }

    // 격리된 재고 조회
    @Override
    @Transactional(readOnly = true)
    public List<IsolatedReservationResult> getIsolatedReservations(UUID requesterId, String requesterRole) {
        UserRole role = authorizationChecker.requireSellerOrAdminRole(requesterRole);
        List<ProductStockReservation> reservations;

        if (role == UserRole.ADMIN) {
            reservations = productStockReservationRepository.findByStatus(ReservationStatus.EXPIRATION_FAILED);
        } else {
            List<UUID> productIds = productReader.getProductIdsBySellerId(requesterId);
            List<UUID> stockIds = productStockRepository.findByProductIdInAndDeletedAtIsNull(productIds)
                    .stream()
                    .map(ProductStock::getId)
                    .toList();
            reservations = productStockReservationRepository.findByStatusAndStockIdIn(ReservationStatus.EXPIRATION_FAILED, stockIds);
        }

        List<UUID> stockIds = reservations.stream()
                .map(ProductStockReservation::getStockId)
                .distinct()
                .toList();
        Map<UUID, ProductStock> stockById = productStockRepository.findAllById(stockIds)
                .stream()
                .collect(Collectors.toMap(ProductStock::getId, s -> s));

        List<IsolatedReservationResult> result = new ArrayList<>();
        for (ProductStockReservation reservation : reservations) {
            ProductStock stock = stockById.get(reservation.getStockId());
            if (stock == null) {
                throw new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_FOUND);
            }
            Product product = productReader.getProduct(stock.getProductId());

            result.add(new IsolatedReservationResult(
                    reservation.getId(),
                    stock.getProductId(),
                    product.getName(),
                    product.getSellerId(),
                    reservation.getOrderId(),
                    reservation.getQuantity(),
                    reservation.getExpiresAt()
            ));
        }
        return result;
    }

    // 격리된 예약 복구
    @Override
    public void recoverIsolatedReservation(UUID reservationId, UUID requesterId, String requesterRole) {
        authorizationChecker.requireAdmin(requesterRole);

        ProductStockReservation reservation = productStockReservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_STOCK_RESERVATION_NOT_FOUND));

        reservation.recoverFromIsolation();
        saveReservationSafely(reservation, ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);

        // 이미 재고 복구(RESTORE)가 실제로 반영됐는지 확인 - 격리 상태 자체가 이걸 몰라서 생긴 것이므로 필수 검증
        boolean alreadyRestored = productStockEventLogRepository
                .findByReservationIdAndEventType(reservation.getId(), StockEventType.RESTORE)
                .isPresent();

        if (alreadyRestored) {
            return; // 재고도 이미 복구됐고 로그도 이미 있으니, 상태 전이만 하고 여기서 끝냄
        }
        ProductStockEventLog reserveLog = productStockEventLogRepository
                .findByReservationIdAndEventType(reservation.getId(), StockEventType.RESERVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_STOCK_RESERVATION_NOT_FOUND));

        ProductStock stock = productStockRepository.findById(reservation.getStockId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_FOUND));
        stock.restore(reservation.getQuantity());
        saveStockSafely(stock, ErrorCode.PRODUCT_STOCK_CONFLICT);

        saveEventLog(reservation.getId(), reserveLog.getOrderItemId(), StockEventType.RESTORE);
    }

    @Override
    public ProductStockTransferResult transferStock(ProductStockTransferCommand command) {
        Product product = productReader.getProduct(command.productId());
        authorizationChecker.requireOwnerOrAdmin(command.requesterId(), command.requesterRole(), product.getSellerId());

        ProductStock stock = productStockRepository.findByProductIdAndDeletedAtIsNull(command.productId())
                .orElseThrow(()->new BusinessException(ErrorCode.PRODUCT_STOCK_NOT_FOUND));

        StockStatus previousStatus = stock.getStatus();
        int quantity = command.quantity();
        int previousAvailable = stock.getAvailableQuantity();
        if (quantity < 0 && product.getStatus() != ProductStatus.ON_SALE) {
            throw new BusinessException(ErrorCode.PRODUCT_STOCK_PRODUCT_NOT_ON_SALE);
        }
        stock.transfer(quantity);

        saveStockSafely(stock, ErrorCode.PRODUCT_STOCK_CONFLICT);
        productStockAllocationLogRepository.save(
                ProductStockAllocationLog.create(
                        stock.getId(),
                        quantity < 0 ? AllocationEventType.ALLOCATE : AllocationEventType.DEALLOCATE,
                        Math.abs(quantity)
                )
        );
        if (stock.getStatus() == StockStatus.SOLD_OUT) {
            notifySoldOut(command.productId());
        } else if (previousStatus == StockStatus.SOLD_OUT) {
            notifyRestocked(command.productId());
        }

        int appliedQuantity = stock.getAvailableQuantity() - previousAvailable;
        return ProductStockTransferResult.of(product.getId(), appliedQuantity, stock.getAvailableQuantity());
    }


    // 재고 예약
    @Override
    public void reserve(UUID orderId, List<ProductStockReserveItem> items) {
        Instant expiresAt = Instant.now().plus(reservationTtl);
        // 1. 멱등성 체크 - IN절 한 번
        List<ProductStockReserveItem> targetItems =
                filterUnprocessed(items, ProductStockReserveItem::orderItemId, List.of(StockEventType.RESERVE));
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
                filterUnprocessed(items, ProductStockItem::orderItemId,List.of(StockEventType.CONFIRM));
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
        // 1. 멱등성 체크 - RESTORE/REFUND 둘 다 이미 처리된 건 제외 (IN절 한 번)
        List<ProductStockItem> targetItems = filterUnprocessed(
                items, ProductStockItem::orderItemId,
                List.of(StockEventType.RESTORE, StockEventType.REFUND)
        );
        if (targetItems.isEmpty()) {
            return;
        }

        // 2. RESERVE 로그 조회 + 예약 조회 - IN절 두 번 (존재 검증 포함, loadValidatedReservations)
        List<UUID> targetOrderItemIds = targetItems.stream().map(ProductStockItem::orderItemId).toList();
        Map<UUID, ProductStockReservation> reservationByOrderItemId = loadValidatedReservations(targetOrderItemIds);

        // 3. reservation 검증 + 상태별 분리
        //    EXPIRED -> skip / EXPIRATION_FAILED -> 즉시 예외 / RESERVED -> 취소 대상 / CONFIRMED -> 환불 대상
        List<ProductStockItem> actuallyRestoreItems = new ArrayList<>();
        List<ProductStockItem> actuallyRefundItems = new ArrayList<>();
        for (ProductStockItem item : targetItems) {
            ProductStockReservation reservation = reservationByOrderItemId.get(item.orderItemId());
            if (reservation == null || !reservation.getOrderId().equals(orderId)) {
                throw new BusinessException(ErrorCode.PRODUCT_STOCK_RESERVATION_NOT_FOUND);
            }


            switch (reservation.getStatus()) {
                case EXPIRED -> { /* 스케줄러가 이미 처리, 멱등 스킵 */ }
                case EXPIRATION_FAILED -> {
                    log.warn("[restore] 격리된 예약 - orderId={}, orderItemId={}, reservationId={}",
                            orderId, item.orderItemId(), reservation.getId());
                    throw new BusinessException(ErrorCode.PRODUCT_STOCK_RESERVATION_ISOLATED);
                }
                case RESERVED -> actuallyRestoreItems.add(item);
                case CONFIRMED -> actuallyRefundItems.add(item);
                default -> throw new BusinessException(ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);
            }
        }

        if (actuallyRestoreItems.isEmpty() && actuallyRefundItems.isEmpty()) {
            return;
        }

        // 4. 재고 조회 - 두 리스트 합쳐서 IN절 한 번
        List<UUID> stockIds = Stream.concat(actuallyRestoreItems.stream(), actuallyRefundItems.stream())
                .map(item -> reservationByOrderItemId.get(item.orderItemId()).getStockId())
                .distinct()
                .toList();
        Map<UUID, ProductStock> stockById = productStockRepository
                .findAllById(stockIds)
                .stream()
                .collect(Collectors.toMap(ProductStock::getId, s -> s));
        Map<UUID, StockStatus> previousStatusByStockId = stockById.values().stream()
                .collect(Collectors.toMap(ProductStock::getId, ProductStock::getStatus));
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
        for (ProductStockItem item : actuallyRefundItems) {
            ProductStockReservation reservation = reservationByOrderItemId.get(item.orderItemId());
            reservation.cancelConfirmed();
            reservationsToSave.add(reservation);

            ProductStock stock = stockById.get(reservation.getStockId());
            if (stock == null || !stock.getProductId().equals(item.productId())) {
                throw new BusinessException(ErrorCode.PRODUCT_STOCK_RESERVATION_NOT_FOUND);
            }
            stock.refundConfirmed(reservation.getQuantity());

            eventLogsToSave.add(ProductStockEventLog.create(reservation.getId(), item.orderItemId(), StockEventType.REFUND));
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
        Set<UUID> restockedProductIds = stockById.values().stream()
                .filter(stock -> previousStatusByStockId.get(stock.getId()) == StockStatus.SOLD_OUT
                        && stock.getStatus() != StockStatus.SOLD_OUT)
                .map(ProductStock::getProductId)
                .collect(Collectors.toSet());
        restockedProductIds.forEach(this::notifyRestocked);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductStock> getStockList(UUID requesterId, String requesterRole, Pageable pageable) {
        UserRole role = authorizationChecker.requireSellerOrAdminRole(requesterRole);
        if (role == UserRole.ADMIN) {
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
        productStockAllocationLogRepository.save(
                ProductStockAllocationLog.create(stock.getId(), AllocationEventType.ALLOCATE, command.quantity())
        );
        if (stock.getStatus() == StockStatus.SOLD_OUT) {
            notifySoldOut(command.productId());
        }
        ProductImageResult image = productImagePort == null
                ? null
                : productImagePort.findImage(product.getId()).orElse(null);
        return new ProductStockAllocateResult(
                product.getId(),
                product.getSellerId(),
                image == null ? null : image.imageId(),
                image == null ? null : image.imageUrl(),
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
        productStockAllocationLogRepository.save(
                ProductStockAllocationLog.create(stock.getId(), AllocationEventType.DEALLOCATE, command.quantity())
        );
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
            List<StockEventType> eventTypes
    ) {
        List<UUID> orderItemIds = items.stream().map(orderItemIdExtractor).toList();
        Set<UUID> alreadyProcessed = productStockEventLogRepository
                .findByOrderItemIdInAndEventTypeIn(orderItemIds, eventTypes)
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

    // 낙관적 락 검증 (예약)
    private void saveReservationSafely(ProductStockReservation reservation, ErrorCode conflictErrorCode) {
        try {
            productStockReservationRepository.saveAndFlush(reservation);
        } catch (OptimisticLockingFailureException e) {
            throw new BusinessException(conflictErrorCode);
        }
    }

    // 이벤트로그 단건 저장 - 유니크 제약 위반만 별도로 좁게 catch
    private void saveEventLog(UUID reservationId, UUID orderItemId, StockEventType eventType) {
        ProductStockEventLog eventLog = ProductStockEventLog.create(reservationId, orderItemId, eventType);
        try {
            productStockEventLogRepository.saveAndFlush(eventLog);
        } catch (DataIntegrityViolationException e) {
            log.warn("[recoverIsolatedReservation] 이벤트로그 유니크 제약 위반 - reservationId={}, orderItemId={}",
                    reservationId, orderItemId);
            throw new BusinessException(ErrorCode.PRODUCT_STOCK_RESERVATION_ALREADY_PROCESSED);
        }
    }
}
