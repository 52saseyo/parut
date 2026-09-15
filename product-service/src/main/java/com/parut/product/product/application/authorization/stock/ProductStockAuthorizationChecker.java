package com.parut.product.product.application.authorization.stock;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductStockAuthorizationChecker {
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String SELLER_ROLE = "SELLER";

    public void requireOwner(UUID requesterId, String requesterRole, UUID sellerId) {
        if (!SELLER_ROLE.equals(requesterRole) || !sellerId.equals(requesterId)) {
            throw new BusinessException(ErrorCode.PRODUCT_STOCK_FORBIDDEN);
        }
    }

    public void requireOwnerOrAdmin(UUID requesterId, String requesterRole, UUID sellerId) {
        if (ADMIN_ROLE.equals(requesterRole)) {
            return;
        }
        requireOwner(requesterId, requesterRole, sellerId);
    }

    public boolean isAdmin(String requesterRole) {
        return ADMIN_ROLE.equals(requesterRole);
    }
}
