package com.parut.product.timedeal.application.authorization;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 타임딜 유스케이스의 역할, 소유권 인가 정책.
 * 인증된 요청자와 조회한 타임딜의 sellerId를 전달한다. JWT 검증, 조회, 판매 상태 검증은 맡지 않는다.
 */
@Component
public class TimeDealAuthorizationChecker {

    private static final String ADMIN = "ADMIN";
    private static final String SELLER = "SELLER";

    // 생성처럼 아직 소유권을 검사할 대상이 없는 운영 유스케이스에 사용한다.
    public void requireSellerOrAdmin(String role) {
        if (ADMIN.equals(role)) {
            return;
        }
        requireSeller(role);
    }

    public void requireSeller(String role) {
        if (!SELLER.equals(role)) {
            throw accessDenied();
        }
    }

    public void requireOwner(UUID requesterId, UUID sellerId) {
        if (requesterId == null || !requesterId.equals(sellerId)) {
            throw accessDenied();
        }
    }

    // ADMIN 예외가 없는 엄격한 판매자 본인 전용 검사다.
    public void requireSellerOwner(UUID requesterId, String role, UUID sellerId) {
        requireSeller(role);
        requireOwner(requesterId, sellerId);
    }

    public void requireAdmin(String role) {
        if (!ADMIN.equals(role)) {
            throw accessDenied();
        }
    }

    // 대상 sellerId는 요청 Body가 아니라 저장소에서 조회한 타임딜의 값이어야 한다.
    public void requireSellerOwnerOrAdmin(UUID requesterId, String role, UUID sellerId) {
        // 요청자 정보의 필수 검증은 요청 경계에서 보장한다. ADMIN은 소유권 검사를 생략한다.
        if (ADMIN.equals(role)) {
            return;
        }
        requireSellerOwner(requesterId, role, sellerId);
    }

    private static BusinessException accessDenied() {
        return new BusinessException(ErrorCode.TIME_DEAL_ACCESS_DENIED);
    }
}
