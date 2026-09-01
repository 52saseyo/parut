package com.parut.product.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.UUID;

/**
 * BaseEntity(id, created 정보)에 더해, 수정/논리삭제 정보까지 필요한 엔티티가 상속받는 클래스.
 * 예: ProductStock, ProductStockReservation처럼 이후 값이 변경되거나 소프트 삭제되는 엔티티.
 * 반대로 ProductStockEventLog처럼 한 번 쓰이고 변경되지 않는 로그성 엔티티는
 * 이 클래스가 아니라 BaseEntity만 상속받는다.
 */
@MappedSuperclass
@Getter
public abstract class BaseUpdatableEntity extends BaseEntity{
    @LastModifiedDate // 엔티티가 수정(update)될 때마다 현재 시각으로 자동 갱신됨
    @Column(name = "updated_at")
    private Instant updatedAt;

    @LastModifiedBy // 엔티티가 수정될 때마다 현재 요청자를 자동으로 채워줌 (AuditorAware 필요)
    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    // 논리 삭제(soft delete) 시각. Auditing 대상이 아니라 softDelete() 메서드로 직접 채움
    private Instant deletedAt;

    @Column(name = "deleted_by")
    // 논리 삭제를 수행한 사용자 ID
    private UUID deletedBy;

    /**
     * 논리 삭제 처리 메서드.
     * deletedAt/deletedBy를 외부에서 setter로 직접 건드리지 않고,
     * 이 메서드를 통해서만 값이 바뀌도록 캡슐화한다.
     */
    public void softDelete(UUID deletedBy) {
        this.deletedAt = Instant.now();
        this.deletedBy = deletedBy;
    }
}
