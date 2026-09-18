package com.parut.product.product.application.authorization.product;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ProductAuthorizationChecker {
    private static final String SELLER = "SELLER";
    private static final String ADMIN = "ADMIN";

    /**
     * 판매자 역할이 필요한 작업에 사용한다.
     * 예: 상품 생성, 판매자 상품 목록 조회
     */
    public void requireSeller(String requesterRole) {
        if (!SELLER.equals(requesterRole)) {
            throw accessDenied();
        }
    }

    /**
     * 판매자 또는 관리자 역할이 필요한 작업에 사용한다.
     */
    public void requireSellerOrAdmin(String requesterRole) {
        if (!SELLER.equals(requesterRole)
                && !ADMIN.equals(requesterRole)) {
            throw accessDenied();
        }
    }

    /**
     * 요청자가 상품 소유자인지 검사한다.
     */
    public void requireOwner(
            UUID requesterId,
            UUID productSellerId
    ) {
        if (requesterId == null || !requesterId.equals(productSellerId)) {
            throw accessDenied();
        }
    }

    /**
     * 판매자 본인만 가능한 작업에 사용한다.
     */
    public void requireSellerOwner(
            UUID requesterId,
            String requesterRole,
            UUID productSellerId
    ) {
        requireSeller(requesterRole);
        requireOwner(requesterId, productSellerId);
    }

    /**
     * 상품 소유 판매자 또는 관리자가 가능한 작업에 사용한다.
     */
    public void requireSellerOwnerOrAdmin(
            UUID requesterId,
            String requesterRole,
            UUID productSellerId
    ) {
        if (ADMIN.equals(requesterRole)) {
            return;
        }

        requireSellerOwner(
                requesterId,
                requesterRole,
                productSellerId
        );
    }

    /**
     * 관리자 전용 작업에 사용한다.
     */
    public void requireAdmin(String requesterRole) {
        if (!ADMIN.equals(requesterRole)) {
            throw accessDenied();
        }
    }

    public boolean isAdmin(String requesterRole) {
        return ADMIN.equals(requesterRole);
    }

    private static BusinessException accessDenied() {
        return new BusinessException(
                ErrorCode.PRODUCT_ACCESS_DENIED
        );
    }
}
