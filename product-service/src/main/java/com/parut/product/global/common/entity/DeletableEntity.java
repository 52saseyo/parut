package com.parut.product.global.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.Instant;

@MappedSuperclass
@Getter
public abstract class DeletableEntity extends UpdatableEntity{
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private String deletedBy;

    public void softDelete(String deletedBy) {
        if (isDeleted()) {
            throw new IllegalStateException("이미 삭제된 데이터입니다.");
        }
        this.deletedAt = Instant.now();
        this.deletedBy = deletedBy;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
