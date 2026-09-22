package com.parut.user.address.application.dto.response;

import com.parut.user.address.domain.UserAddress;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.UUID;

// 로컬 프로필의 공통 SNAKE_CASE 설정과 무관하게 배송지 계약을 camelCase로 고정합니다.
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public record AddressResponse(
        UUID addressId,
        String addressName,
        String recipientName,
        String recipientPhone,
        String zipCode,
        String addressBase,
        String addressDetail,
        boolean defaultAddress
) {
    public static AddressResponse from(UserAddress address) {
        return new AddressResponse(
                address.getId(),
                address.getAddressName(),
                address.getRecipientName(),
                address.getRecipientPhone(),
                address.getZipCode(),
                address.getAddressBase(),
                address.getAddressDetail(),
                address.isDefault()
        );
    }
}
