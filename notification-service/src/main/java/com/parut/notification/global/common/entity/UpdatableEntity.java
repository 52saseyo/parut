package com.parut.notification.global.common.entity;

import java.time.Instant;

import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class UpdatableEntity extends BaseEntity {

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

}
