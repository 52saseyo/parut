package com.parut.user.address.application.dto.request;

import jakarta.validation.constraints.Size;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

// 생략한 필드는 null로 도착해 기존 값을 유지합니다. 기본 배송지 여부는 전용 API에서만 변경합니다.
// @Size는 null을 통과시키므로 생략 계약을 유지하면서 길이만 제한합니다.
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public record AddressUpdateRequest(
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
        String addressDetail
) {
}
