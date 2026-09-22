package com.parut.user.address.application.service;

import com.parut.user.address.application.dto.request.AddressCreateRequest;
import com.parut.user.address.application.dto.request.AddressUpdateRequest;
import com.parut.user.address.application.dto.response.AddressDeleteResponse;
import com.parut.user.address.application.dto.response.AddressResponse;
import com.parut.user.address.domain.UserAddress;
import com.parut.user.address.infrastructure.UserAddressRepository;
import com.parut.user.global.exception.BusinessException;
import com.parut.user.global.exception.ErrorCode;
import com.parut.user.user.domain.User;
import com.parut.user.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAddressService {

    private static final String DEFAULT_ADDRESS_CONSTRAINT = "uq_p_user_addresses_default_active";

    private final UserAddressRepository userAddressRepository;
    private final UserRepository userRepository;

    // 1. 배송지 목록 조회 - 기본 배송지를 먼저 보여주고 나머지는 최근 등록 순으로 정렬
    public List<AddressResponse> getAddresses(UUID userId) {
        return userAddressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDescIdDesc(userId)
                .stream()
                .map(AddressResponse::from)
                .toList();
    }

    // 2. 배송지 등록 - 첫 배송지는 요청값과 관계없이 기본 배송지로 지정
    @Transactional
    public AddressResponse createAddress(UUID userId, AddressCreateRequest request) {
        // 배송지는 사용자에게 귀속되므로 등록 시점에만 사용자 존재를 확인하고 감사 주체(username)를 확보합니다.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        String actor = user.getUsername();

        boolean firstAddress = !userAddressRepository.existsByUserId(userId);
        boolean markDefault = firstAddress || Boolean.TRUE.equals(request.defaultAddress());

        // 첫 배송지는 해제할 기존 기본 배송지가 없으므로 조회와 flush를 생략합니다.
        if (markDefault && !firstAddress) {
            releaseCurrentDefault(userId, actor);
        }

        UserAddress address = new UserAddress(
                userId,
                request.addressName(),
                request.recipientName(),
                request.recipientPhone(),
                request.zipCode(),
                request.addressBase(),
                request.addressDetail(),
                markDefault,
                actor
        );

        return AddressResponse.from(saveDefaultSafely(address));
    }

    // 3. 배송지 부분 수정 - 기본 배송지 여부는 변경하지 않음
    @Transactional
    public AddressResponse updateAddress(
            UUID userId,
            UUID addressId,
            AddressUpdateRequest request
    ) {
        UserAddress address = getOwnedAddress(userId, addressId);

        address.update(
                request.addressName(),
                request.recipientName(),
                request.recipientPhone(),
                request.zipCode(),
                request.addressBase(),
                request.addressDetail(),
                // 감사 주체는 등록 시 확보한 소유자 username을 재사용합니다.
                address.getCreatedBy()
        );

        return AddressResponse.from(address);
    }

    // 4. 기본 배송지 변경 - 기존 기본 배송지 해제와 신규 지정을 한 트랜잭션에서 처리
    @Transactional
    public AddressResponse changeDefaultAddress(UUID userId, UUID addressId) {
        UserAddress address = getOwnedAddress(userId, addressId);

        if (address.isDefault()) {
            return AddressResponse.from(address);
        }

        String actor = address.getCreatedBy();
        releaseCurrentDefault(userId, actor);
        address.markDefault(actor);

        return AddressResponse.from(saveDefaultSafely(address));
    }

    // 5. 배송지 삭제 - 기본 배송지를 지우면 남은 배송지 중 최근 등록 건을 기본 배송지로 지정
    @Transactional
    public AddressDeleteResponse deleteAddress(UUID userId, UUID addressId) {
        UserAddress address = getOwnedAddress(userId, addressId);
        String actor = address.getCreatedBy();
        boolean wasDefault = address.isDefault();

        address.softDelete(actor);
        // 삭제를 먼저 반영해야 남은 배송지를 기본으로 올릴 때 기본 배송지 제약과 충돌하지 않습니다.
        userAddressRepository.flush();

        if (wasDefault) {
            userAddressRepository.findFirstByUserIdOrderByCreatedAtDescIdDesc(userId)
                    .ifPresent(next -> {
                        next.markDefault(actor);
                        saveDefaultSafely(next);
                    });
        }

        return AddressDeleteResponse.of(address.getId(), address.getDeletedAt());
    }

    // 다른 사용자의 배송지는 존재 여부를 노출하지 않도록 동일하게 ADDRESS_NOT_FOUND로 응답합니다.
    private UserAddress getOwnedAddress(UUID userId, UUID addressId) {
        return userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUND));
    }

    // 새 기본 배송지를 확정하기 전에 기존 기본 배송지 해제를 먼저 DB에 반영합니다.
    // Hibernate는 INSERT를 UPDATE보다 먼저 flush하므로 명시적 flush가 없으면 부분 유니크 인덱스를 위반할 수 있습니다.
    private void releaseCurrentDefault(UUID userId, String actor) {
        userAddressRepository.findFirstByUserIdAndIsDefaultTrue(userId)
                .ifPresent(current -> current.releaseDefault(actor));

        userAddressRepository.flush();
    }

    // 동시 요청의 최종 방어선은 부분 유니크 인덱스이므로, 커밋까지 미루지 않고 즉시 flush해 409로 변환합니다.
    private UserAddress saveDefaultSafely(UserAddress address) {
        try {
            return userAddressRepository.saveAndFlush(address);
        } catch (DataIntegrityViolationException e) {
            if (isDefaultAddressConflict(e)) {
                throw new BusinessException(ErrorCode.ADDRESS_DEFAULT_CONFLICT);
            }
            throw e;
        }
    }

    private boolean isDefaultAddressConflict(Throwable throwable) {
        Throwable cause = throwable;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && DEFAULT_ADDRESS_CONSTRAINT.equals(constraintViolation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
