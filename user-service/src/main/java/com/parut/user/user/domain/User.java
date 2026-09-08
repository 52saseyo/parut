package com.parut.user.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_users")
@SQLRestriction("deleted_at IS NULL") // Soft Delete 처리
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 20, nullable = false, unique = true)
    private String username;

    @Column(length = 100, nullable = false)
    private String password;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(name = "slack_id", length = 100)
    private String slackId;

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
    private String role = "CUSTOMER"; // 기본값 설정
    //  --END

    // Entity 내부에 추가할 비즈니스 메서드
    public void updateProfile(String name, String slackId, String updatedBy) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (slackId != null && !slackId.isBlank()) {
            this.slackId = slackId;
        }

        this.updatedAt = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        this.updatedBy = updatedBy;
    }

    public void softDelete(String deletedBy) {
        this.deletedAt = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        this.deletedBy = deletedBy;
    }

    // 회원가입 시 사용할 생성자 추가
    public User(String username, String password, String name, String slackId, String createdBy) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.slackId = slackId;
        this.createdBy = createdBy;
        this.createdAt = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
    }
}