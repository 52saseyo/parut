package com.parut.product.image.application.authorization;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ImageAuthorizationChecker {

    private static final String SELLER = "SELLER";
    private static final String ADMIN = "ADMIN";

    /**
     * 이미지를 업로드할 수 있는 역할인지 검사한다.
     */
    public void requireUploaderRole(String requesterRole) {
        if (!SELLER.equals(requesterRole)
                && !ADMIN.equals(requesterRole)) {
            throw accessDenied();
        }
    }

    /**
     * 요청자가 이미지를 업로드한 사용자인지 검사한다.
     */
    public void requireOwner(
            UUID requesterId,
            UUID imageUploaderId
    ) {
        if (requesterId == null
                || !requesterId.equals(imageUploaderId)) {
            throw accessDenied();
        }
    }

    /**
     * 업로더 본인만 허용한다.
     */
    public void requireUploaderOwner(
            UUID requesterId,
            String requesterRole,
            UUID imageUploaderId
    ) {
        requireUploaderRole(requesterRole);
        requireOwner(requesterId, imageUploaderId);
    }

    /**
     * 업로더 본인 또는 관리자를 허용한다.
     */
    public void requireUploaderOwnerOrAdmin(
            UUID requesterId,
            String requesterRole,
            UUID imageUploaderId
    ) {
        if (ADMIN.equals(requesterRole)) {
            return;
        }

        requireUploaderOwner(
                requesterId,
                requesterRole,
                imageUploaderId
        );
    }

    private BusinessException accessDenied() {
        return new BusinessException(
                ErrorCode.IMAGE_ACCESS_DENIED
        );
    }
}
