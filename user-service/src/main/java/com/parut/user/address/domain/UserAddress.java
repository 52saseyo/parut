package com.parut.user.address.domain;

import com.parut.user.global.exception.BusinessException;
import com.parut.user.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_user_addresses")
@SQLRestriction("deleted_at IS NULL") // Soft Delete 처리
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAddress {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "address_name", length = 30, nullable = false)
    private String addressName;

    @Column(name = "recipient_name", length = 50, nullable = false)
    private String recipientName;

    @Column(name = "recipient_phone", length = 20, nullable = false)
    private String recipientPhone;

    @Column(name = "zip_code", length = 10, nullable = false)
    private String zipCode;

    @Column(name = "address_base", length = 255, nullable = false)
    private String addressBase;

    @Column(name = "address_detail", length = 255)
    private String addressDetail;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "created_by", length = 50, nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "deleted_at")
    private ZonedDateTime deletedAt;

    @Column(name = "deleted_by", length = 50)
    private String deletedBy;

    // 배송지 등록 시 사용할 생성자
    public UserAddress(
            UUID userId,
            String addressName,
            String recipientName,
            String recipientPhone,
            String zipCode,
            String addressBase,
            String addressDetail,
            boolean isDefault,
            String createdBy
    ) {
        this.userId = userId;
        this.addressName = requireText(addressName);
        this.recipientName = requireText(recipientName);
        this.recipientPhone = requireText(recipientPhone);
        this.zipCode = requireText(zipCode);
        this.addressBase = requireText(addressBase);
        this.addressDetail = normalizeDetail(addressDetail);
        this.isDefault = isDefault;
        this.createdBy = createdBy;
        this.createdAt = ZonedDateTime.now(KST);
    }

    /**
     * 부분 수정 규칙
     * null 은 기존 값을 유지한다.
     * 필수 문자열은 빈 문자열이나 공백으로 바꿀 수 없다.
     * 선택 항목인 addressDetail 만 빈 문자열로 제거할 수 있다.
     * 기본 배송지 여부는 이 메서드에서 변경하지 않는다.
     */
    public void update(
            String addressName,
            String recipientName,
            String recipientPhone,
            String zipCode,
            String addressBase,
            String addressDetail,
            String updatedBy
    ) {
        if (addressName != null) {
            this.addressName = requireText(addressName);
        }
        if (recipientName != null) {
            this.recipientName = requireText(recipientName);
        }
        if (recipientPhone != null) {
            this.recipientPhone = requireText(recipientPhone);
        }
        if (zipCode != null) {
            this.zipCode = requireText(zipCode);
        }
        if (addressBase != null) {
            this.addressBase = requireText(addressBase);
        }
        if (addressDetail != null) {
            this.addressDetail = normalizeDetail(addressDetail);
        }

        touch(updatedBy);
    }

    public void markDefault(String updatedBy) {
        if (this.isDefault) {
            return;
        }
        this.isDefault = true;
        touch(updatedBy);
    }

    public void releaseDefault(String updatedBy) {
        if (!this.isDefault) {
            return;
        }
        this.isDefault = false;
        touch(updatedBy);
    }

    public void softDelete(String deletedBy) {
        this.deletedAt = ZonedDateTime.now(KST);
        this.deletedBy = deletedBy;
    }

    private void touch(String updatedBy) {
        this.updatedAt = ZonedDateTime.now(KST);
        this.updatedBy = updatedBy;
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return value.trim();
    }

    // 상세 주소는 선택 항목이므로 빈 값이면 저장하지 않는다.
    private static String normalizeDetail(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
