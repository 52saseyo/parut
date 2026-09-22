package com.parut.user.address.presentation;

import com.parut.user.address.application.dto.request.AddressCreateRequest;
import com.parut.user.address.application.dto.response.AddressDeleteResponse;
import com.parut.user.address.application.dto.response.AddressResponse;
import com.parut.user.address.application.service.UserAddressService;
import com.parut.user.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserAddressControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ADDRESS_ID = UUID.randomUUID();

    @Mock
    private UserAddressService userAddressService;

    @InjectMocks
    private UserAddressController userAddressController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userAddressController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private AddressResponse response(boolean defaultAddress) {
        return new AddressResponse(
                ADDRESS_ID,
                "집",
                "홍길동",
                "010-1234-5678",
                "06236",
                "서울특별시 강남구 테헤란로 123",
                "101동 101호",
                defaultAddress
        );
    }

    @Test
    @DisplayName("고객이 아닌 역할은 배송지 API를 사용할 수 없다")
    void rejectNonCustomer() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/addresses")
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "SELLER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_ACCESS_DENIED"));
    }

    @Test
    @DisplayName("배송지 등록은 camelCase 요청을 역직렬화하고 201을 반환한다")
    void createAddress() throws Exception {
        given(userAddressService.createAddress(eq(USER_ID), any(AddressCreateRequest.class)))
                .willReturn(response(true));

        String body = """
                {
                  "addressName": "집",
                  "recipientName": "홍길동",
                  "recipientPhone": "010-1234-5678",
                  "zipCode": "06236",
                  "addressBase": "서울특별시 강남구 테헤란로 123",
                  "addressDetail": "101동 101호",
                  "defaultAddress": true
                }
                """;

        mockMvc.perform(post("/api/v1/users/me/addresses")
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.defaultAddress").value(true));

        verify(userAddressService).createAddress(eq(USER_ID), any(AddressCreateRequest.class));
    }

    @Test
    @DisplayName("배송지 삭제는 200과 삭제 응답 객체를 반환한다")
    void deleteAddress() throws Exception {
        given(userAddressService.deleteAddress(USER_ID, ADDRESS_ID))
                .willReturn(AddressDeleteResponse.of(
                        ADDRESS_ID,
                        ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
                ));

        mockMvc.perform(delete("/api/v1/users/me/addresses/{addressId}", ADDRESS_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.addressId").value(ADDRESS_ID.toString()))
                .andExpect(jsonPath("$.data.deletedAt").exists());
    }

    @Test
    @DisplayName("등록 요청의 길이 초과는 DB까지 가지 않고 400으로 거부한다")
    void rejectTooLongFieldOnCreate() throws Exception {
        String body = createBody("가".repeat(31));

        mockMvc.perform(post("/api/v1/users/me/addresses")
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));

        verify(userAddressService, never()).createAddress(any(), any());
    }

    @Test
    @DisplayName("수정 요청의 길이 초과도 400으로 거부한다")
    void rejectTooLongFieldOnUpdate() throws Exception {
        String body = "{\"addressBase\": \"" + "가".repeat(256) + "\"}";

        mockMvc.perform(patch("/api/v1/users/me/addresses/{addressId}", ADDRESS_ID)
                        .header("X-User-Id", USER_ID.toString())
                        .header("X-User-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));

        verify(userAddressService, never()).updateAddress(any(), any(), any());
    }

    private String createBody(String addressName) {
        return """
                {
                  "addressName": "%s",
                  "recipientName": "홍길동",
                  "recipientPhone": "010-1234-5678",
                  "zipCode": "06236",
                  "addressBase": "서울특별시 강남구 테헤란로 123",
                  "addressDetail": "101동 101호",
                  "defaultAddress": true
                }
                """.formatted(addressName);
    }
}
