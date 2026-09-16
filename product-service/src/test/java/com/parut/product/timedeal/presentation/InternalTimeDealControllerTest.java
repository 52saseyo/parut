package com.parut.product.timedeal.presentation;

import com.parut.product.global.interceptor.ServiceKeyInterceptor;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealDetailResult;
import com.parut.product.timedeal.application.port.in.timedeal.TimeDealQueryUseCase;
import com.parut.product.timedeal.application.port.in.timedealpurchase.TimeDealPurchaseCommandUseCase;
import com.parut.product.timedeal.domain.timedeal.TimeDealProductGrade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = InternalTimeDealController.class, properties = "internal.service-key=test-service-key")
@Import(ServiceKeyInterceptor.class)
class InternalTimeDealControllerTest {
    @MockitoBean
    private TimeDealQueryUseCase timeDealQueryUseCase;

    @MockitoBean
    private TimeDealPurchaseCommandUseCase timeDealPurchaseCommandUseCase;

    @Autowired
    private MockMvc mvc;

    @Nested
    @DisplayName("내부 단건 조회")
    class GetDetail {
        @Test
        void 설명과_할인율을_포함한_상품정보를_반환한다() throws Exception {
            UUID timeDealId = UUID.randomUUID();
            when(timeDealQueryUseCase.getDetail(timeDealId)).thenReturn(
                    new TimeDealDetailResult(timeDealId, UUID.randomUUID(), UUID.randomUUID(), null,
                            "산지직송 감자 3kg", "테스트용 타임딜입니다.", 15000L,
                            new BigDecimal("20.00"), 12000L, TimeDealProductGrade.NORMAL,
                            "국내산(전남 해남)", LocalDate.of(2026, 8, 20)));
            mvc.perform(get("/api/v1/internal/time-deals/{id}", timeDealId)
                            .header("X-Service-Key", "test-service-key"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OK"))
                    .andExpect(jsonPath("$.data.timeDealId").value(timeDealId.toString()))
                    .andExpect(jsonPath("$.data.productName").value("산지직송 감자 3kg"))
                    .andExpect(jsonPath("$.data.description").value("테스트용 타임딜입니다."))
                    .andExpect(jsonPath("$.data.originalPrice").value(15000))
                    .andExpect(jsonPath("$.data.discountRate").value(20.00))
                    .andExpect(jsonPath("$.data.dealPrice").value(12000))
                    .andExpect(jsonPath("$.data.productGrade").value("NORMAL"))
                    .andExpect(jsonPath("$.data.origin").value("국내산(전남 해남)"))
                    .andExpect(jsonPath("$.data.harvestedDate").value("2026-08-20"))
                    .andExpect(jsonPath("$.traceId").isString());
        }

        @Test
        void 설명이_없으면_null이며_추적_ID를_그대로_반환한다() throws Exception {
            UUID timeDealId = UUID.randomUUID();
            when(timeDealQueryUseCase.getDetail(timeDealId)).thenReturn(
                    new TimeDealDetailResult(timeDealId, null, UUID.randomUUID(), null,
                            "감자", null, 15000L, BigDecimal.ZERO, 15000L,
                            TimeDealProductGrade.NORMAL, "국내산", LocalDate.of(2026, 8, 20)));
            mvc.perform(get("/api/v1/internal/time-deals/{id}", timeDealId)
                            .header("X-Service-Key", "test-service-key")
                            .header("X-Trace-Id", "trace-123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.description").value(nullValue()))
                    .andExpect(jsonPath("$.data.discountRate").value(0))
                    .andExpect(jsonPath("$.traceId").value("trace-123"));
        }

        @Test
        void 서비스_키가_없으면_조회를_거절한다() throws Exception {
            mvc.perform(get("/api/v1/internal/time-deals/{id}", UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
            verifyNoInteractions(timeDealQueryUseCase);
        }
    }
}
