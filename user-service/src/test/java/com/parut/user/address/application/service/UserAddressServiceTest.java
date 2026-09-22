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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.hibernate.exception.ConstraintViolationException;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserAddressServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final String USERNAME = "parut";

    @Mock
    private UserAddressRepository userAddressRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserAddressService userAddressService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(USERNAME, "encoded", "홍길동", null, USERNAME);
    }

    private UserAddress address(String addressName, boolean isDefault) {
        UserAddress address = new UserAddress(
                USER_ID,
                addressName,
                "홍길동",
                "010-1234-5678",
                "06236",
                "서울특별시 강남구 테헤란로 123",
                "101동 101호",
                isDefault,
                USERNAME
        );
        ReflectionTestUtils.setField(address, "id", UUID.randomUUID());
        return address;
    }

    private AddressCreateRequest createRequest(Boolean defaultAddress) {
        return new AddressCreateRequest(
                "집",
                "홍길동",
                "010-1234-5678",
                "06236",
                "서울특별시 강남구 테헤란로 123",
                "101동 101호",
                defaultAddress
        );
    }

    private void stubSaveAndFlush() {
        given(userAddressRepository.saveAndFlush(any(UserAddress.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("첫 배송지는 요청값과 관계없이 기본 배송지로 지정한다")
    void firstAddressBecomesDefault() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(userAddressRepository.existsByUserId(USER_ID)).willReturn(false);
        stubSaveAndFlush();

        AddressResponse response = userAddressService.createAddress(USER_ID, createRequest(false));

        assertThat(response.defaultAddress()).isTrue();
        // 해제할 기존 기본 배송지가 없으므로 조회하지 않는다
        verify(userAddressRepository, never()).findFirstByUserIdAndIsDefaultTrue(USER_ID);
    }

    @Test
    @DisplayName("일반 배송지를 추가하면 기존 기본 배송지를 유지한다")
    void keepExistingDefaultWhenAddingNormalAddress() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(userAddressRepository.existsByUserId(USER_ID)).willReturn(true);
        stubSaveAndFlush();

        AddressResponse response = userAddressService.createAddress(USER_ID, createRequest(false));

        assertThat(response.defaultAddress()).isFalse();
        verify(userAddressRepository, never()).findFirstByUserIdAndIsDefaultTrue(USER_ID);
    }

    @Test
    @DisplayName("기본 배송지로 등록하면 기존 기본 배송지를 해제하고 먼저 flush 한다")
    void releaseExistingDefaultWhenRegisteringDefault() {
        UserAddress existingDefault = address("회사", true);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(userAddressRepository.existsByUserId(USER_ID)).willReturn(true);
        given(userAddressRepository.findFirstByUserIdAndIsDefaultTrue(USER_ID))
                .willReturn(Optional.of(existingDefault));
        stubSaveAndFlush();

        AddressResponse response = userAddressService.createAddress(USER_ID, createRequest(true));

        assertThat(response.defaultAddress()).isTrue();
        assertThat(existingDefault.isDefault()).isFalse();

        InOrder order = inOrder(userAddressRepository);
        order.verify(userAddressRepository).flush();
        order.verify(userAddressRepository).saveAndFlush(any(UserAddress.class));
    }

    @Test
    @DisplayName("다른 사용자의 배송지는 수정할 수 없다")
    void rejectUpdateForOtherUser() {
        UUID addressId = UUID.randomUUID();
        given(userAddressRepository.findByIdAndUserId(addressId, OTHER_USER_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> userAddressService.updateAddress(
                OTHER_USER_ID,
                addressId,
                new AddressUpdateRequest(null, "김파릇", null, null, null, null)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ADDRESS_NOT_FOUND);
    }

    @Test
    @DisplayName("기본 배송지를 삭제하면 남은 최근 배송지를 기본 배송지로 지정한다")
    void promoteLatestAddressAfterDeletingDefault() {
        UserAddress target = address("회사", true);
        UserAddress remaining = address("집", false);
        given(userAddressRepository.findByIdAndUserId(target.getId(), USER_ID))
                .willReturn(Optional.of(target));
        given(userAddressRepository.findFirstByUserIdOrderByCreatedAtDescIdDesc(USER_ID))
                .willReturn(Optional.of(remaining));
        stubSaveAndFlush();

        AddressDeleteResponse response = userAddressService.deleteAddress(USER_ID, target.getId());

        assertThat(response.addressId()).isEqualTo(target.getId());
        assertThat(response.deletedAt()).isNotNull();
        assertThat(remaining.isDefault()).isTrue();

        InOrder order = inOrder(userAddressRepository);
        order.verify(userAddressRepository).flush();
        order.verify(userAddressRepository).findFirstByUserIdOrderByCreatedAtDescIdDesc(USER_ID);
        order.verify(userAddressRepository).saveAndFlush(remaining);
    }

    @Test
    @DisplayName("마지막 배송지를 삭제하면 기본 배송지 없이 종료한다")
    void deleteLastAddress() {
        UserAddress target = address("집", true);
        given(userAddressRepository.findByIdAndUserId(target.getId(), USER_ID))
                .willReturn(Optional.of(target));
        given(userAddressRepository.findFirstByUserIdOrderByCreatedAtDescIdDesc(USER_ID))
                .willReturn(Optional.empty());

        AddressDeleteResponse response = userAddressService.deleteAddress(USER_ID, target.getId());

        assertThat(response.deletedAt()).isNotNull();
        verify(userAddressRepository, never()).saveAndFlush(any(UserAddress.class));
    }

    @Test
    @DisplayName("기본 배송지 제약 위반은 409 배송지 예외로 변환한다")
    void convertConstraintViolationToConflict() {
        UserAddress target = address("집", false);
        given(userAddressRepository.findByIdAndUserId(target.getId(), USER_ID))
                .willReturn(Optional.of(target));
        given(userAddressRepository.findFirstByUserIdAndIsDefaultTrue(USER_ID))
                .willReturn(Optional.empty());
        given(userAddressRepository.saveAndFlush(any(UserAddress.class)))
                .willThrow(defaultAddressConstraintViolation());

        assertThatThrownBy(() -> userAddressService.changeDefaultAddress(USER_ID, target.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ADDRESS_DEFAULT_CONFLICT);
    }

    @Test
    @DisplayName("기본 배송지 제약이 아닌 무결성 오류는 그대로 전파한다")
    void rethrowOtherConstraintViolation() {
        UserAddress target = address("집", false);
        given(userAddressRepository.findByIdAndUserId(target.getId(), USER_ID))
                .willReturn(Optional.of(target));
        given(userAddressRepository.findFirstByUserIdAndIsDefaultTrue(USER_ID))
                .willReturn(Optional.empty());
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "other constraint",
                new ConstraintViolationException("other constraint", new SQLException(), "fk_p_user_addresses_user")
        );
        given(userAddressRepository.saveAndFlush(any(UserAddress.class))).willThrow(exception);

        assertThatThrownBy(() -> userAddressService.changeDefaultAddress(USER_ID, target.getId()))
                .isSameAs(exception);
    }

    private DataIntegrityViolationException defaultAddressConstraintViolation() {
        return new DataIntegrityViolationException(
                "default address constraint",
                new ConstraintViolationException(
                        "default address constraint",
                        new SQLException(),
                        "uq_p_user_addresses_default_active"
                )
        );
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 배송지를 등록할 수 없다")
    void rejectCreateForUnknownUser() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userAddressService.createAddress(USER_ID, createRequest(true)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}
