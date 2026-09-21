package com.parut.product.timedeal.infrastructure.persistence.timedeal;

import com.parut.product.timedeal.application.dto.timedeal.TimeDealDetailView;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealPublicDetailView;
import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import com.parut.product.timedeal.domain.timedeal.TimeDealStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.domain.Pageable;


public interface JpaTimeDealQueryRepository extends JpaRepository<TimeDeal, UUID> {

    @Query("""
            select new com.parut.product.timedeal.application.dto.timedeal.TimeDealPublicDetailView(
                       t.id, t.productId, t.sellerId, t.name, t.description,
                       t.productGrade, t.origin, t.harvestedDate, t.originalPrice, t.discountRate,
                       t.dealPrice, t.startAt, t.endAt, t.maxPurchaseQuantity, t.status,
                       s.availableQuantity, s.reservedQuantity, s.soldQuantity, s.lowStockThreshold)
              from TimeDeal t
              join TimeDealStock s on s.timeDealId = t.id
             where t.deletedAt is null
               and s.deletedAt is null
               and t.status = :status
             order by t.startAt desc, t.id desc
            """)
    List<TimeDealPublicDetailView> findFirstPublicList(
            @Param("status") TimeDealStatus status,
            Pageable pageable
    );

    @Query("""
            select new com.parut.product.timedeal.application.dto.timedeal.TimeDealPublicDetailView(
                       t.id, t.productId, t.sellerId, t.name, t.description,
                       t.productGrade, t.origin, t.harvestedDate, t.originalPrice, t.discountRate,
                       t.dealPrice, t.startAt, t.endAt, t.maxPurchaseQuantity, t.status,
                       s.availableQuantity, s.reservedQuantity, s.soldQuantity, s.lowStockThreshold)
              from TimeDeal t
              join TimeDealStock s on s.timeDealId = t.id
             where t.deletedAt is null
               and s.deletedAt is null
               and t.status = :status
               and (t.startAt < :cursor
                    or (t.startAt = :cursor and t.id < :cursorId))
             order by t.startAt desc, t.id desc
            """)
    List<TimeDealPublicDetailView> findNextPublicList(
            @Param("status") TimeDealStatus status,
            @Param("cursor") Instant cursor,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    @Query("""
            select new com.parut.product.timedeal.application.dto.timedeal.TimeDealPublicDetailView(
                       t.id, t.productId, t.sellerId, t.name, t.description,
                       t.productGrade, t.origin, t.harvestedDate, t.originalPrice, t.discountRate,
                       t.dealPrice, t.startAt, t.endAt, t.maxPurchaseQuantity, t.status,
                       s.availableQuantity, s.reservedQuantity, s.soldQuantity, s.lowStockThreshold)
              from TimeDeal t
              join TimeDealStock s on s.timeDealId = t.id
             where t.sellerId = :sellerId
               and t.deletedAt is null
               and s.deletedAt is null
             order by t.startAt desc, t.id desc
            """)
    List<TimeDealPublicDetailView> findFirstSellerList(
            @Param("sellerId") UUID sellerId,
            Pageable pageable
    );

    @Query("""
            select new com.parut.product.timedeal.application.dto.timedeal.TimeDealPublicDetailView(
                       t.id, t.productId, t.sellerId, t.name, t.description,
                       t.productGrade, t.origin, t.harvestedDate, t.originalPrice, t.discountRate,
                       t.dealPrice, t.startAt, t.endAt, t.maxPurchaseQuantity, t.status,
                       s.availableQuantity, s.reservedQuantity, s.soldQuantity, s.lowStockThreshold)
              from TimeDeal t
              join TimeDealStock s on s.timeDealId = t.id
             where t.sellerId = :sellerId
               and t.deletedAt is null
               and s.deletedAt is null
               and (t.startAt < :cursor
                    or (t.startAt = :cursor and t.id < :cursorId))
             order by t.startAt desc, t.id desc
            """)
    List<TimeDealPublicDetailView> findNextSellerList(
            @Param("sellerId") UUID sellerId,
            @Param("cursor") Instant cursor,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    // NOTE: 판매 조건과 재고를 한 번에 조회하고, 삭제된 타임딜·재고는 제외한다.
    @Query("""
            select new com.parut.product.timedeal.application.dto.timedeal.TimeDealPublicDetailView(
                       t.id, t.productId, t.sellerId, t.name, t.description,
                       t.productGrade, t.origin, t.harvestedDate, t.originalPrice, t.discountRate,
                       t.dealPrice, t.startAt, t.endAt,
                       t.maxPurchaseQuantity, t.status,
                       s.availableQuantity, s.reservedQuantity, s.soldQuantity, s.lowStockThreshold)
              from TimeDeal t
              join TimeDealStock s on s.timeDealId = t.id
             where t.id = :timeDealId
               and t.deletedAt is null
               and s.deletedAt is null
            """)
    Optional<TimeDealPublicDetailView> findPublicDetailById(@Param("timeDealId") UUID timeDealId);

    // NOTE: 엔티티를 로드하지 않고 View를 직접 채운다. 삭제된 타임딜은 조회 단계에서 걸러낸다.
    @Query("""
            select new com.parut.product.timedeal.application.dto.timedeal.TimeDealDetailView(
                       t.id,
                       t.productId,
                       t.sellerId,
                       t.name,
                       t.description,
                       t.originalPrice,
                       t.discountRate,
                       t.dealPrice,
                       t.productGrade,
                       t.origin,
                       t.harvestedDate)
              from TimeDeal t
             where t.id = :timeDealId
               and t.deletedAt is null
            """)
    Optional<TimeDealDetailView> findDetailById(@Param("timeDealId") UUID timeDealId);
}
