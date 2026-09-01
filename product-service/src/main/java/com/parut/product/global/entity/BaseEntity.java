package com.parut.product.global.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * 모든 엔티티가 공통으로 갖는 최소 공통 필드(PK, 생성 정보)를 정의하는 부모 클래스.
 * 실제 테이블로 생성되지 않고, 상속받는 자식 엔티티의 컬럼으로 합쳐진다.
 *
 * 수정/삭제가 필요 없는 엔티티(예: 이벤트 로그성 테이블)는 이 클래스만 상속받고,
 * 수정/삭제가 필요한 엔티티는 이걸 상속받는 BaseUpdatableEntity를 상속받는다.
 */

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @Id // 기본 키(PK) 지정
    @GeneratedValue(strategy = GenerationType.UUID) // Hibernate가 UUID를 자동 생성 (Hibernate 6+ 지원)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    // columnDefinition = "uuid" : DB 컬럼 타입을 PostgreSQL 네이티브 UUID로 명시
    // updatable = false : PK는 이후 수정 불가
    private UUID id;

    @CreatedDate // 엔티티가 최초 저장(insert)될 때 현재 시각을 자동으로 채워줌
    @Column(name = "created_at", updatable = false, nullable = false)
    // updatable = false : 생성 시각은 이후 절대 수정되지 않음
    private Instant createdAt;

    @CreatedBy // 엔티티가 최초 저장될 때, 현재 요청자(AuditorAware 구현체가 알려주는 값)를 자동으로 채워줌
    @Column(name = "created_by", updatable = false)
    // 실제로 값이 채워지려면 별도로 AuditorAware<UUID> 구현체를 빈으로 등록해야 함
    // (예: Gateway가 넘겨주는 X-User-Id 헤더를 읽어서 반환하는 구현체)
    private UUID createdBy;
}
