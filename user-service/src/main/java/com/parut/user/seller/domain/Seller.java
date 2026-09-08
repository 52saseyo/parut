package com.parut.user.seller.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_sellers")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "login_id", length = 20, nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Column(name = "biz_reg_no", nullable = false, length = 20, unique = true)
    private String bizRegNo;

    @Column(name = "rep_name", nullable = false, length = 50)
    private String repName;

    @Column(name = "biz_address", nullable = false, length = 255)
    private String bizAddress;

    @Column(name = "manager_name", nullable = false, length = 50)
    private String managerName;

    @Column(name = "manager_phone", nullable = false, length = 20)
    private String managerPhone;

    @Column(name = "manager_email", nullable = false, length = 100)
    private String managerEmail;

    @Column(name = "slack_id", length = 100)
    private String slackId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SellerStatus status;

    @Column(name = "approved_by", length = 30)
    private String approvedBy;

    @Column(name = "approved_at")
    private ZonedDateTime approvedAt;

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

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

    @Column(name = "role", nullable = false, length = 30)
    private String role = "SELLER"; // 기본값 설정

    @Builder
    public Seller(String loginId, String password, String companyName, String bizRegNo,
                  String repName, String bizAddress, String managerName, String managerPhone,
                  String managerEmail, String slackId) {
        this.loginId = loginId;
        this.password = password;
        this.companyName = companyName;
        this.bizRegNo = bizRegNo;
        this.repName = repName;
        this.bizAddress = bizAddress;
        this.managerName = managerName;
        this.managerPhone = managerPhone;
        this.managerEmail = managerEmail;
        this.slackId = slackId;
        this.status = SellerStatus.PENDING;
        this.createdAt = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        this.createdBy = loginId;
    }

    public void updateInfo(String companyName, String bizAddress, String managerName,
                           String managerPhone, String managerEmail, String slackId, String updatedBy) {
        if (companyName != null && !companyName.isBlank()) this.companyName = companyName;
        if (bizAddress != null && !bizAddress.isBlank()) this.bizAddress = bizAddress;
        if (managerName != null && !managerName.isBlank()) this.managerName = managerName;
        if (managerPhone != null && !managerPhone.isBlank()) this.managerPhone = managerPhone;
        if (managerEmail != null && !managerEmail.isBlank()) this.managerEmail = managerEmail;
        if (slackId != null) this.slackId = slackId;
        this.updatedAt = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        this.updatedBy = updatedBy;
    }

    public void approve(String adminName) {
        this.status = SellerStatus.APPROVED;
        this.approvedBy = adminName;
        this.approvedAt = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        this.updatedAt = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        this.updatedBy = adminName;
    }

    public void reject(String adminName, String rejectReason) {
        this.status = SellerStatus.REJECTED;
        this.approvedBy = adminName;
        this.approvedAt = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        this.rejectReason = rejectReason;
        this.updatedAt = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        this.updatedBy = adminName;
    }

    public void softDelete(String deletedBy) {
        this.deletedAt = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        this.deletedBy = deletedBy;
    }
}