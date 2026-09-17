package com.parut.product.product.application.authorization.stock;

import com.parut.product.global.common.UserRole;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductStockAuthorizationChecker {

    public void requireOwner(UUID requesterId, String requesterRole, UUID sellerId) {
        UserRole role = UserRole.parse(requesterRole).orElseThrow(this::forbidden);
        if (role != UserRole.SELLER || !sellerId.equals(requesterId)) {
            throw forbidden();
        }
    }

    public void requireOwnerOrAdmin(UUID requesterId, String requesterRole, UUID sellerId) {
        UserRole role = UserRole.parse(requesterRole).orElseThrow(this::forbidden);
        if (role == UserRole.ADMIN) {
            return;
        }
        requireOwner(requesterId, requesterRole, sellerId);
    }

    public boolean isAdmin(String requesterRole) {
        return UserRole.parse(requesterRole).map(r -> r == UserRole.ADMIN).orElse(false);
    }

    public void requireAdmin(String requesterRole) {
        if (!isAdmin(requesterRole)) {
            throw forbidden();
        }
    }
    public UserRole requireSellerOrAdminRole(String requesterRole) {
        UserRole role = UserRole.parse(requesterRole).orElseThrow(this::forbidden);
        if (role != UserRole.SELLER && role != UserRole.ADMIN) {
            throw forbidden();
        }
        return role;
    }

    private BusinessException forbidden() {
        return new BusinessException(ErrorCode.PRODUCT_STOCK_FORBIDDEN);
    }
}
