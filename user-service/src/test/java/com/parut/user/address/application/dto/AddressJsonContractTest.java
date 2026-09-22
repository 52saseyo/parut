package com.parut.user.address.application.dto;

import com.parut.user.address.application.dto.request.AddressUpdateRequest;
import com.parut.user.address.application.dto.response.AddressResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 로컬 프로필은 공통 Jackson 설정이 SNAKE_CASE 입니다.
 * 배송지 DTO는 타입 단위 설정으로 camelCase를 고정하므로, SNAKE_CASE 매퍼에서도 계약이 유지되어야 합니다.
 */
class AddressJsonContractTest {

    private final JsonMapper snakeCaseMapper = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();

    @Test
    @DisplayName("SNAKE_CASE 설정에서도 응답은 camelCase로 직렬화된다")
    void serializeResponseAsCamelCase() {
        AddressResponse response = new AddressResponse(
                UUID.randomUUID(),
                "집",
                "홍길동",
                "010-1234-5678",
                "06236",
                "서울특별시 강남구 테헤란로 123",
                "101동 101호",
                true
        );

        String json = snakeCaseMapper.writeValueAsString(response);

        assertThat(json)
                .contains("\"addressId\"")
                .contains("\"addressName\"")
                .contains("\"recipientName\"")
                .contains("\"recipientPhone\"")
                .contains("\"zipCode\"")
                .contains("\"addressBase\"")
                .contains("\"addressDetail\"")
                .contains("\"defaultAddress\"");
        assertThat(json)
                .doesNotContain("address_id")
                .doesNotContain("address_name")
                .doesNotContain("recipient_name")
                .doesNotContain("default_address");
    }

    @Test
    @DisplayName("생략한 수정 필드는 null로 역직렬화되어 기존 값을 유지한다")
    void deserializePartialUpdateRequest() {
        String json = """
                {
                  "recipientName": "김파릇"
                }
                """;

        AddressUpdateRequest request = snakeCaseMapper.readValue(json, AddressUpdateRequest.class);

        assertThat(request.recipientName()).isEqualTo("김파릇");
        assertThat(request.addressName()).isNull();
        assertThat(request.zipCode()).isNull();
        assertThat(request.addressBase()).isNull();
        assertThat(request.addressDetail()).isNull();
    }
}
