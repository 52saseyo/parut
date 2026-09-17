package com.parut.user.user.presentation;

import com.parut.user.user.application.dto.response.UserVerifyResponse;
import com.parut.user.user.application.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
public class UserInternalController {

    private final UserService userService;

    // Gateway 전용 검증 API
    @GetMapping("/{userId}/verify")
    public ResponseEntity<UserVerifyResponse> verifyUser(
            @PathVariable String userId
    ) {
        UserVerifyResponse response = userService.verifyUserStatus(userId);
        return ResponseEntity.ok(response);
    }
}
