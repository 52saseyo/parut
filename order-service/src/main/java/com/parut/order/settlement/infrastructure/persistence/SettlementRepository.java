package com.parut.order.settlement.infrastructure.persistence;

import java.util.UUID;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.parut.order.settlement.domain.Settlement;
import com.parut.order.settlement.domain.SettlementStatus;

/**
 * 정산 목록을 생성 시각 내림차순으로 조회한다.
 *
 * <p>생성 시각이 같은 정산은 ID 내림차순을 보조 기준으로 사용해 Cursor 경계를 고정한다.
 */
public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

    @Query("""
            SELECT settlement FROM Settlement settlement
            WHERE settlement.sellerId = :sellerId
              AND settlement.status = :status
              AND (:cursor IS NULL OR settlement.createdAt < :cursor
                   OR (settlement.createdAt = :cursor AND settlement.id < :cursorId))
            ORDER BY settlement.createdAt DESC, settlement.id DESC
            """)
    List<Settlement> findSellerSettlements(
            @Param("sellerId") UUID sellerId,
            @Param("status") SettlementStatus status,
            @Param("cursor") Instant cursor,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    @Query("""
            SELECT settlement FROM Settlement settlement
            WHERE settlement.status = :status
              AND (:sellerId IS NULL OR settlement.sellerId = :sellerId)
              AND (:cursor IS NULL OR settlement.createdAt < :cursor
                   OR (settlement.createdAt = :cursor AND settlement.id < :cursorId))
            ORDER BY settlement.createdAt DESC, settlement.id DESC
            """)
    List<Settlement> findAdminSettlements(
            @Param("sellerId") UUID sellerId,
            @Param("status") SettlementStatus status,
            @Param("cursor") Instant cursor,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );
}
