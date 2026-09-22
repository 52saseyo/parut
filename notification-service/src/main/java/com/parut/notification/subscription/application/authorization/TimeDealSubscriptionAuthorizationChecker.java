package com.parut.notification.subscription.application.authorization;

import com.parut.notification.global.common.UserRole;
import com.parut.notification.global.exception.BusinessException;
import com.parut.notification.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class TimeDealSubscriptionAuthorizationChecker {
    /**
     * 타임딜 알림 신청 기능은 CUSTOMER만 허용합니다.
     */
    public void requireCustomer(
            String requesterRole
    ) {
        UserRole role = UserRole.parse(requesterRole)
                        .orElseThrow(TimeDealSubscriptionAuthorizationChecker::accessDenied);

        if (role != UserRole.CUSTOMER) {
            throw accessDenied();
        }
    }

    private static BusinessException accessDenied() {
        return new BusinessException(
                ErrorCode.TIME_DEAL_SUBSCRIPTION_ACCESS_DENIED
        );
    }
}
