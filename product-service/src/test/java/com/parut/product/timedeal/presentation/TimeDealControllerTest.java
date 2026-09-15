package com.parut.product.timedeal.presentation;

import com.parut.product.timedeal.application.dto.timedeal.TimeDealDeleteCommand;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealStopCommand;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealStopResult;
import com.parut.product.timedeal.application.port.in.timedeal.TimeDealCommandUseCase;
import com.parut.product.timedeal.application.port.in.timedeal.TimeDealQueryUseCase;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealPublicDetailView;
import com.parut.product.timedeal.domain.timedeal.TimeDealStatus;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.global.interceptor.ServiceKeyInterceptor;
import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import com.parut.product.timedeal.domain.timedeal.TimeDealProductGrade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TimeDealController.class, properties = "internal.service-key=test-service-key")
@Import(ServiceKeyInterceptor.class)
class TimeDealControllerTest {
    @MockitoBean
    private TimeDealCommandUseCase useCase;

    @MockitoBean
    private TimeDealQueryUseCase queryUseCase;

    @Autowired
    private MockMvc mvc;

    @Nested
    @DisplayName("공개 단건 조회")
    class GetDetail {
        @Test
        void 사용자_헤더_없이_판매조건과_재고정보를_조회한다() throws Exception {
            UUID timeDealId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            when(queryUseCase.getPublicDetail(timeDealId)).thenReturn(
                    new TimeDealPublicDetailView(timeDealId, productId,
                            UUID.fromString("11111111-1111-4111-8111-111111111111"),
                            UUID.fromString("22222222-2222-4222-8222-222222222222"),
                            "테스트 사과", "산지직송 사과입니다.", TimeDealProductGrade.UGLY, "경북 안동",
                            LocalDate.of(2026, 9, 1), 25000L, new BigDecimal("20.40"), 19900L,
                            Instant.parse("2026-09-01T14:00:00Z"), Instant.parse("2026-09-01T17:00:00Z"),
                            3, TimeDealStatus.SCHEDULED, 100, 0, 0, 10));
            mvc.perform(get("/api/v1/time-deals/{id}", timeDealId).header("X-Trace-Id", "trace-123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OK"))
                    .andExpect(jsonPath("$.data.timeDealId").value(timeDealId.toString()))
                    .andExpect(jsonPath("$.data.productId").value(productId.toString()))
                    .andExpect(jsonPath("$.data.sellerId").value("11111111-1111-4111-8111-111111111111"))
                    .andExpect(jsonPath("$.data.imageId").value("22222222-2222-4222-8222-222222222222"))
                    .andExpect(jsonPath("$.data.name").value("테스트 사과"))
                    .andExpect(jsonPath("$.data.description").value("산지직송 사과입니다."))
                    .andExpect(jsonPath("$.data.productGrade").value("UGLY"))
                    .andExpect(jsonPath("$.data.origin").value("경북 안동"))
                    .andExpect(jsonPath("$.data.harvestedDate").value("2026-09-01"))
                    .andExpect(jsonPath("$.data.originalPrice").value(25000))
                    .andExpect(jsonPath("$.data.discountRate").value(20.40))
                    .andExpect(jsonPath("$.data.dealPrice").value(19900))
                    .andExpect(jsonPath("$.data.startAt").value("2026-09-01T14:00:00Z"))
                    .andExpect(jsonPath("$.data.endAt").value("2026-09-01T17:00:00Z"))
                    .andExpect(jsonPath("$.data.maxPurchaseQuantity").value(3))
                    .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                    .andExpect(jsonPath("$.data.stock.availableQuantity").value(100))
                    .andExpect(jsonPath("$.data.stock.reservedQuantity").value(0))
                    .andExpect(jsonPath("$.data.stock.soldQuantity").value(0))
                    .andExpect(jsonPath("$.data.stock.lowStockThreshold").value(10))
                    .andExpect(jsonPath("$.traceId").value("trace-123"))
                    .andExpect(jsonPath("$.timestamp").exists());
            verifyNoInteractions(useCase);
        }

        @Test
        void 직접등록_타임딜은_productId가_null이며_추적헤더도_생략할수있다() throws Exception {
            UUID timeDealId = UUID.randomUUID();
            when(queryUseCase.getPublicDetail(timeDealId)).thenReturn(
                    new TimeDealPublicDetailView(timeDealId, null, UUID.randomUUID(), null,
                            "직접 등록 사과", null, TimeDealProductGrade.NORMAL, "경북 안동",
                            LocalDate.of(2026, 9, 1), 25000L, new BigDecimal("20.40"), 19900L,
                            Instant.now(), Instant.now().plusSeconds(3600),
                            3, TimeDealStatus.SCHEDULED, 100, 0, 0, 10));
            mvc.perform(get("/api/v1/time-deals/{id}", timeDealId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.productId").value(nullValue()))
                    .andExpect(jsonPath("$.data.imageId").value(nullValue()))
                    .andExpect(jsonPath("$.data.description").value(nullValue()))
                    .andExpect(jsonPath("$.traceId").isString());
        }

        @Test
        void 조회할_타임딜이_없으면_404를_반환한다() throws Exception {
            UUID timeDealId = UUID.randomUUID();
            when(queryUseCase.getPublicDetail(timeDealId))
                    .thenThrow(new BusinessException(ErrorCode.TIME_DEAL_NOT_FOUND));
            mvc.perform(get("/api/v1/time-deals/{id}", timeDealId)
                            .header("X-Trace-Id", "trace-error-404"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("TIME_DEAL_NOT_FOUND"))
                    .andExpect(jsonPath("$.traceId").value("trace-error-404"));
        }
    }

    @Nested
    @DisplayName("타임딜 삭제")
    class Delete {
        @Test
        void 삭제_성공은_200과_null_data_및_요청_traceId를_반환한다() throws Exception {
            UUID id = UUID.randomUUID();
            UUID requesterId = UUID.randomUUID();
            mvc.perform(delete("/api/v1/time-deals/{id}", id)
                            .header("X-User-Id", requesterId)
                            .header("X-User-Role", "SELLER")
                            .header("X-Trace-Id", "trace-123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OK"))
                    .andExpect(jsonPath("$.data").value(nullValue()))
                    .andExpect(jsonPath("$.traceId").value("trace-123"))
                    .andExpect(jsonPath("$.timestamp").exists());
            verify(useCase).delete(new TimeDealDeleteCommand(id, requesterId, "SELLER"));
        }

        @Test
        void traceId가_없어도_삭제할_수_있다() throws Exception {
            mvc.perform(delete("/api/v1/time-deals/{id}", UUID.randomUUID())
                            .header("X-User-Id", UUID.randomUUID())
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.traceId").isString());
        }
    }

    @Nested
    @DisplayName("타임딜 강제 종료")
    class Stop {
        @Test
        void 강제종료_성공은_STOPPED_상태와_traceId를_반환한다() throws Exception {
            UUID id = UUID.randomUUID();
            UUID requesterId = UUID.randomUUID();
            when(useCase.stop(new TimeDealStopCommand(id, requesterId, "SELLER")))
                    .thenReturn(new TimeDealStopResult(id, TimeDealStatus.STOPPED));

            mvc.perform(patch("/api/v1/time-deals/{id}/stop", id)
                            .header("X-User-Id", requesterId)
                            .header("X-User-Role", "SELLER")
                            .header("X-Trace-Id", "trace-stop-123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OK"))
                    .andExpect(jsonPath("$.data.timeDealId").value(id.toString()))
                    .andExpect(jsonPath("$.data.status").value("STOPPED"))
                    .andExpect(jsonPath("$.traceId").value("trace-stop-123"))
                    .andExpect(jsonPath("$.timestamp").exists());
            verify(useCase).stop(new TimeDealStopCommand(id, requesterId, "SELLER"));
        }

        @Test
        void traceId가_없어도_강제종료할_수_있다() throws Exception {
            UUID id = UUID.randomUUID();
            UUID requesterId = UUID.randomUUID();
            when(useCase.stop(new TimeDealStopCommand(id, requesterId, "ADMIN")))
                    .thenReturn(new TimeDealStopResult(id, TimeDealStatus.STOPPED));

            mvc.perform(patch("/api/v1/time-deals/{id}/stop", id)
                            .header("X-User-Id", requesterId)
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("STOPPED"))
                    .andExpect(jsonPath("$.traceId").isString());
        }
    }
}
