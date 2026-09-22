package com.parut.notification.notification.presentation.support;

import com.parut.notification.global.exception.BusinessException;
import com.parut.notification.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class NotificationCursorValidator {

    public void validate(
            Instant cursor,
            UUID cursorId,
            int size
    ) {
        validateCursor(cursor, cursorId);
        validateSize(size);
    }

    private void validateCursor(
            Instant cursor,
            UUID cursorId
    ) {
        boolean onlyOneProvided =
                (cursor == null) != (cursorId == null);

        if (onlyOneProvided) {
            throw new BusinessException(ErrorCode.INVALID_NOTIFICATION_CURSOR);
        }
    }

    private void validateSize(int size) {
        if (size != 10
                && size != 30
                && size != 50) {
            throw new BusinessException(ErrorCode.INVALID_PAGE_SIZE);
        }
    }
}
