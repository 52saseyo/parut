package com.parut.product.timedeal.presentation;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockQueryResult;
import com.parut.product.timedeal.application.port.in.timedealstock.TimeDealStockCommandUseCase;
import com.parut.product.timedeal.application.port.in.timedealstock.TimeDealStockQueryUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TimeDealStockController.class)
class TimeDealStockControllerTest {

    @MockitoBean
    private TimeDealStockQueryUseCase useCase;

    @MockitoBean
    private TimeDealStockCommandUseCase commandUseCase;

    @Autowired
    private MockMvc mvc;

    @Test
    void 사용자_헤더로_타임딜_재고를_조회한다() throws Exception {
        UUID timeDealId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        when(useCase.getStock(timeDealId, requesterId, "SELLER"))
                .thenReturn(new TimeDealStockQueryResult(timeDealId, 90, 5, 25, 10));

        mvc.perform(get("/api/v1/time-deals/{timeDealId}/stock", timeDealId)
                        .header("X-User-Id", requesterId)
                        .header("X-User-Role", "SELLER")
                        .header("X-Trace-Id", "trace-stock-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.timeDealId").value(timeDealId.toString()))
                .andExpect(jsonPath("$.data.availableQuantity").value(90))
                .andExpect(jsonPath("$.data.reservedQuantity").value(5))
                .andExpect(jsonPath("$.data.soldQuantity").value(25))
                .andExpect(jsonPath("$.data.lowStockThreshold").value(10))
                .andExpect(jsonPath("$.traceId").value("trace-stock-123"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(useCase).getStock(timeDealId, requesterId, "SELLER");
    }

    @Test
    void 타임딜이_없으면_404를_반환한다() throws Exception {
        UUID timeDealId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        when(useCase.getStock(timeDealId, requesterId, "SELLER"))
                .thenThrow(new BusinessException(ErrorCode.TIME_DEAL_NOT_FOUND));

        mvc.perform(get("/api/v1/time-deals/{timeDealId}/stock", timeDealId)
                        .header("X-User-Id", requesterId)
                        .header("X-User-Role", "SELLER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TIME_DEAL_NOT_FOUND"));
    }

    @Test
    void 사용자_헤더가_없으면_400을_반환한다() throws Exception {
        mvc.perform(get("/api/v1/time-deals/{timeDealId}/stock", UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }
}
