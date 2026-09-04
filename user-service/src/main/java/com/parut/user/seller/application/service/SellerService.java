package com.parut.user.seller.application.service;

import com.parut.user.seller.application.dto.request.SellerApplicationProcessRequest;
import com.parut.user.seller.application.dto.request.SellerApplyRequest;
import com.parut.user.seller.application.dto.request.SellerUpdateRequest;
import com.parut.user.seller.application.dto.response.SellerApplicationStatusResponse;
import com.parut.user.seller.application.dto.response.SellerDeleteResponse;
import com.parut.user.seller.application.dto.response.SellerResponse;
import com.parut.user.seller.domain.Seller;
import com.parut.user.seller.domain.SellerStatus;
import com.parut.user.seller.infrastructure.SellerRepository;
import com.parut.user.global.common.OffsetPageInfo;
import com.parut.user.global.common.OffsetResponse;
import com.parut.user.global.common.SortDirection;
import com.parut.user.global.exception.BusinessException;
import com.parut.user.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerService {

    private final SellerRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SellerResponse apply(SellerApplyRequest request) {// 1. 아이디 중복 검증
        if (sellerRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME); // 필요 시 SELLER_DUPLICATE_ID 에러코드 추가
        }

        // 2. 비밀번호 암호화 적용
        String encodedPassword = passwordEncoder.encode(request.password());

        Seller seller = Seller.builder()
                .loginId(request.loginId())
                .password(encodedPassword)  // 암호화된 비밀번호 저장
                .companyName(request.companyName())
                .bizRegNo(request.bizRegNo())
                .repName(request.repName())
                .bizAddress(request.bizAddress())
                .managerName(request.managerName())
                .managerPhone(request.managerPhone())
                .managerEmail(request.managerEmail())
                .slackId(request.slackId())
                .build();

        Seller savedSeller = sellerRepository.save(seller);
        return SellerResponse.from(savedSeller);
    }

    @Transactional(readOnly = true)
    public SellerApplicationStatusResponse getApplicationStatus(UUID sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));
        return SellerApplicationStatusResponse.from(seller);
    }

    @Transactional(readOnly = true)
    public OffsetResponse<SellerResponse> getApplicationList(String keyword, Pageable pageable) {
        Page<Seller> sellerPage;
        if (keyword == null || keyword.isBlank()) {
            sellerPage = sellerRepository.findAll(pageable);
        } else {
            sellerPage = sellerRepository.findByCompanyNameContainingOrManagerNameContaining(keyword, keyword, pageable);
        }

        List<SellerResponse> content = sellerPage.getContent().stream()
                .map(SellerResponse::from)
                .toList();

        String sortProperty = "createdAt";
        SortDirection sortDirection = SortDirection.DESC;
        if (sellerPage.getSort().isSorted()) {
            Sort.Order order = sellerPage.getSort().iterator().next();
            sortProperty = order.getProperty();
            sortDirection = order.isAscending() ? SortDirection.ASC : SortDirection.DESC;
        }

        OffsetPageInfo pageInfo = OffsetPageInfo.of(
                sellerPage.getNumber(),
                sellerPage.getSize(),
                sortProperty,
                sortDirection,
                sellerPage.getTotalElements(),
                sellerPage.getTotalPages(),
                sellerPage.isLast()
        );

        return new OffsetResponse<>(content, pageInfo);
    }

    @Transactional
    public SellerResponse processApplication(UUID applicationId, SellerApplicationProcessRequest request, String adminName) {
        Seller seller = sellerRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        if (request.status() == SellerStatus.APPROVED) {
            seller.approve(adminName);
        } else if (request.status() == SellerStatus.REJECTED) {
            seller.reject(adminName, request.rejectReason());
        }

        return SellerResponse.from(seller);
    }

    @Transactional(readOnly = true)
    public SellerResponse getSeller(UUID sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));
        return SellerResponse.from(seller);
    }

    @Transactional
    public SellerResponse updateSeller(UUID targetId, UUID sellerId, SellerUpdateRequest request) {
        // 1. 권한 검증: 본인만 탈퇴 가능
        if (!targetId.equals(sellerId)) {
            throw new BusinessException(ErrorCode.SELLER_ACCESS_DENIED);
        }

        // 2. 대상 유저 조회
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        seller.updateInfo(
                request.companyName(),
                request.bizAddress(),
                request.managerName(),
                request.managerPhone(),
                request.managerEmail(),
                request.slackId(),
                seller.getLoginId()
        );

        return SellerResponse.from(seller);
    }

    @Transactional
    public SellerDeleteResponse deleteSeller(UUID targetId, UUID sellerId) {
        // 1. 권한 검증: 본인만 탈퇴 가능
        if (!targetId.equals(sellerId)) {
            throw new BusinessException(ErrorCode.SELLER_ACCESS_DENIED);
        }

        // 2. 대상 유저 조회
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        // 3. Soft Delete 상태 변경 (더티 체킹 적용)
        seller.softDelete(seller.getLoginId());

        // TO-DO : ORDER쪽 개발 완료되면 주문중인 데이터가 있는지 확인 후 삭제 진행

        return SellerDeleteResponse.of(seller.getId(), seller.getDeletedAt());
    }
}