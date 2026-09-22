package com.parut.product.timedeal.presentation;

import com.parut.product.timedeal.application.dto.timedeal.TimeDealDeleteCommand;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealCursorResult;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealStopCommand;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealStopResult;
import com.parut.product.timedeal.application.port.in.timedeal.TimeDealCommandUseCase;
import com.parut.product.timedeal.application.port.in.timedeal.TimeDealQueryUseCase;
import com.parut.product.timedeal.domain.timedeal.TimeDealStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SellerTimeDealController.class)
class SellerTimeDealControllerTest {

    @MockitoBean
    private TimeDealCommandUseCase useCase;

    @MockitoBean
    private TimeDealQueryUseCase queryUseCase;

    @Autowired
    private MockMvc mvc;

    @Test
    void 판매자_본인_타임딜_목록을_커서로_조회한다() throws Exception {
        UUID sellerId = UUID.randomUUID();
        when(queryUseCase.getSellerOwnedTimeDealList(sellerId, "SELLER", null, null, 10))
                .thenReturn(TimeDealCursorResult.withoutNextCursor(List.of()));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/time-deals/seller")
                        .header("X-User-Id", sellerId)
                        .header("X-User-Role", "SELLER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.pageInfo.paginationType").value("CURSOR"))
                .andExpect(jsonPath("$.data.pageInfo.sortBy").value("startAt"));

        verify(queryUseCase).getSellerOwnedTimeDealList(sellerId, "SELLER", null, null, 10);
    }

    @Test
    void 판매자_타임딜_삭제() throws Exception {
        UUID id = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        mvc.perform(delete("/api/v1/time-deals/seller/{id}", id)
                        .header("X-User-Id", requesterId)
                        .header("X-User-Role", "SELLER")
                        .header("X-Trace-Id", "trace-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.traceId").value("trace-123"));

        verify(useCase).delete(new TimeDealDeleteCommand(id, requesterId, "SELLER"));
    }

    @Test
    void 판매자_타임딜_강제종료() throws Exception {
        UUID id = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        when(useCase.stop(new TimeDealStopCommand(id, requesterId, "SELLER")))
                .thenReturn(new TimeDealStopResult(id, TimeDealStatus.STOPPED));

        mvc.perform(patch("/api/v1/time-deals/seller/{id}/stop", id)
                        .header("X-User-Id", requesterId)
                        .header("X-User-Role", "SELLER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.timeDealId").value(id.toString()))
                .andExpect(jsonPath("$.data.status").value("STOPPED"));

        verify(useCase).stop(new TimeDealStopCommand(id, requesterId, "SELLER"));
    }
}
