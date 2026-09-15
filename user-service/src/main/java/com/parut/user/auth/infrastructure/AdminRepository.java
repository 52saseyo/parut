package com.parut.user.auth.infrastructure;

import com.parut.user.auth.application.service.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AdminRepository extends JpaRepository<Admin, UUID> {

    /**
     * 로그인 등에 사용될 관리자 조회 메서드
     * 삭제(deleted_at)되지 않은 유효한 계정만 조회합니다.
     */
    Optional<Admin> findByUsernameAndDeletedAtIsNull(String username);
}
