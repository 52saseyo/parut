package com.parut.user.user.application.service;

import com.parut.user.user.application.dto.request.UserUpdateRequest;
import com.parut.user.user.application.dto.response.UserDeleteResponse;
import com.parut.user.user.application.dto.response.UserResponse;
import com.parut.user.user.domain.User;
import com.parut.user.global.common.OffsetPageInfo;
import com.parut.user.global.common.OffsetResponse;
import com.parut.user.global.common.SortDirection;
import com.parut.user.global.exception.BusinessException;
import com.parut.user.global.exception.ErrorCode;
import com.parut.user.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    // 관리자 화면이므로 Offset 방식이 적합하다 판단하여 Offset방식으로 진행
    @Transactional(readOnly = true)
    public OffsetResponse<UserResponse> getUserList(String keyword, Pageable pageable) {
        Page<User> userPage;

        // 1. 데이터 조회
        if (keyword == null || keyword.isBlank()) {
            userPage = userRepository.findAll(pageable);
        } else {
            userPage = userRepository.findByUsernameContainingOrNameContaining(keyword, keyword, pageable);
        }

        // 2. Content(데이터 리스트) 변환
        List<UserResponse> content = userPage.getContent().stream()
                .map(UserResponse::from)
                .toList();

        // 3. 정렬 정보 추출 (첫 번째 정렬 기준 적용)
        String sortProperty = "createdAt";
        SortDirection sortDirection = SortDirection.DESC;

        if (userPage.getSort().isSorted()) {
            Sort.Order order = userPage.getSort().iterator().next();
            sortProperty = order.getProperty();
            sortDirection = order.isAscending() ? SortDirection.ASC : SortDirection.DESC;
        }

        // 4. PageInfo 생성
        OffsetPageInfo pageInfo = OffsetPageInfo.of(
                userPage.getNumber(), // 현재 페이지 (0부터 시작)
                userPage.getSize(),   // 페이지 크기
                sortProperty,         // 정렬 기준 컬럼
                sortDirection,        // 정렬 방향 (팀 Enum)
                userPage.getTotalElements(), // 전체 데이터 수
                userPage.getTotalPages(),    // 전체 페이지 수
                userPage.isLast()            // 마지막 페이지 여부
        );

        return new OffsetResponse<>(content, pageInfo);
    }


    @Transactional
    public UserResponse updateUser(UUID targetId, UUID requesterId, UserUpdateRequest request) {

        // 1. 권한 검증: 타인의 정보를 수정하려는지 확인
        if (!targetId.equals(requesterId)) {
            throw new BusinessException(ErrorCode.USER_ACCESS_DENIED);
        }

        // 2. 수정할 유저 조회
        User user = userRepository.findById(targetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 3. 더티 체킹(Dirty Checking)을 통한 업데이트 수행
        // user.getUsername()을 updatedBy(수정자)로 사용
        user.updateProfile(request.name(), request.slackId(), user.getUsername());

        // 4. 업데이트된 정보를 DTO로 변환하여 반환
        return UserResponse.from(user);
    }


    @Transactional
    public UserDeleteResponse deleteUser(UUID targetId, UUID requesterId) {

        // 1. 권한 검증: 본인만 탈퇴 가능
        if (!targetId.equals(requesterId)) {
            throw new BusinessException(ErrorCode.USER_ACCESS_DENIED);
        }

        // 2. 대상 유저 조회
        User user = userRepository.findById(targetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 3. Soft Delete 상태 변경 (더티 체킹 적용)
        user.softDelete(user.getUsername());

        // TO-DO : ORDER쪽 개발 완료되면 주문중인 데이터가 있는지 확인 후 삭제 진행

        // 4. 탈퇴 응답 DTO 반환
        return UserDeleteResponse.of(user.getId(), user.getDeletedAt());
    }
}
