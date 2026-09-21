package com.parut.notification.notification.application.authorization;

import com.parut.notification.global.common.UserRole;
import com.parut.notification.global.exception.BusinessException;
import com.parut.notification.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
public class NotificationAuthorizationChecker {
    private static final Set<UserRole> ALLOWED_ROLES =
            EnumSet.of(
                    UserRole.CUSTOMER,
                    UserRole.SELLER,
                    UserRole.ADMIN
            );

    /**
     * 알림 기능을 이용할 수 있는 역할인지 확인
     */
    public void requireNotificationAccess(
            String requesterRole
    ) {
        UserRole role =
                UserRole.parse(requesterRole)
                        .orElseThrow(
                                NotificationAuthorizationChecker
                                        ::accessDenied
                        );

        if (!ALLOWED_ROLES.contains(role)) {
            throw accessDenied();
        }
    }

    private static BusinessException accessDenied() {
        return new BusinessException(
                ErrorCode.NOTIFICATION_ACCESS_DENIED
        );
    }
}
