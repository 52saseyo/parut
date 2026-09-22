package com.parut.user.address.application.dto.request;

import jakarta.validation.constraints.Size;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

// 로컬 프로필의 공통 SNAKE_CASE 설정과 무관하게 배송지 계약을 camelCase로 고정합니다.
// 길이 제한은 p_user_addresses 컬럼 길이와 맞춰, 초과 입력이 DB까지 내려가지 않도록 막습니다.
// null과 공백 검증은 UserAddress 생성자가 담당합니다.
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public record AddressCreateRequest(
        @Size(max = 30)
        String addressName,

        @Size(max = 50)
        String recipientName,

        @Size(max = 20)
        String recipientPhone,

        @Size(max = 10)
        String zipCode,

        @Size(max = 255)
        String addressBase,

        @Size(max = 255)
        String addressDetail,

        Boolean defaultAddress
) {
}
