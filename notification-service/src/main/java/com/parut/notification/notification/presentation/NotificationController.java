package com.parut.notification.notification.presentation;


import com.parut.notification.global.common.ApiResponse;
import com.parut.notification.global.common.CursorPageInfo;
import com.parut.notification.global.common.CursorResponse;
import com.parut.notification.global.common.SortDirection;
import com.parut.notification.global.constant.HeaderConstants;
import com.parut.notification.global.logging.TraceIdContext;
import com.parut.notification.notification.application.dto.NotificationCursorResult;
import com.parut.notification.notification.application.port.in.NotificationCommandUseCase;
import com.parut.notification.notification.application.port.in.NotificationQueryUseCase;
import com.parut.notification.notification.domain.NotificationType;
import com.parut.notification.notification.presentation.dto.response.NotificationResponse;
import com.parut.notification.notification.presentation.dto.response.UnreadNotificationCountResponse;
import com.parut.notification.notification.presentation.support.NotificationCursorValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private static final String SORT_BY = "createdAt";

    private final NotificationQueryUseCase queryUseCase;
    private final NotificationCommandUseCase commandUseCase;
    private final NotificationCursorValidator cursorValidator;


    @GetMapping
    public ResponseEntity<ApiResponse<CursorResponse<NotificationResponse>>>
    getNotifications(
            @RequestHeader(HeaderConstants.USER_ID) UUID userId,
            @RequestHeader(HeaderConstants.USER_ROLE) String userRole,
            @RequestParam(required = false) Instant cursor,
            @RequestParam(required = false) UUID cursorId,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) NotificationType type
    ){
        cursorValidator.validate(cursor, cursorId, size);

        NotificationCursorResult result =
                queryUseCase.getNotifications(
                        userId,
                        userRole,
                        cursor,
                        cursorId,
                        size,
                        isRead,
                        type
                );

        List<NotificationResponse> content = result.content().stream()
                .map(NotificationResponse::from)
                .toList();

        CursorPageInfo pageInfo = CursorPageInfo.of(
                result.nextCursorCreatedAt() == null
                        ? null
                        : result.nextCursorCreatedAt().toString(),
                result.nextCursorId(),
                result.hasNext(),
                SORT_BY,
                SortDirection.DESC
        );


        CursorResponse<NotificationResponse> response = new CursorResponse<>(content, pageInfo);
        return ResponseEntity.ok(ApiResponse.success(response, TraceIdContext.currentTraceId()));
    }


    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadNotificationCountResponse>> getUnreadCount(
            @RequestHeader(HeaderConstants.USER_ID) UUID userId,
            @RequestHeader(HeaderConstants.USER_ROLE) String userRole
    ){
        long unreadCount = queryUseCase.getUnreadCount(userId, userRole);
        UnreadNotificationCountResponse response = UnreadNotificationCountResponse.from(unreadCount);
        return ResponseEntity.ok(ApiResponse.success(response, TraceIdContext.currentTraceId()));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> read(
            @RequestHeader(HeaderConstants.USER_ID) UUID userId,
            @RequestHeader(HeaderConstants.USER_ROLE) String userRole,
            @PathVariable UUID notificationId
    ){
        commandUseCase.read(userId, userRole, notificationId);
        return ResponseEntity.ok(ApiResponse.success(null, TraceIdContext.currentTraceId()));
    }

}
