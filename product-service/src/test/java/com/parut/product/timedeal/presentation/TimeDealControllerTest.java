package com.parut.product.timedeal.presentation;

import com.parut.product.timedeal.application.dto.timedeal.TimeDealDeleteCommand;
import com.parut.product.timedeal.application.port.in.timedeal.TimeDealCommandUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TimeDealControllerTest {
    private TimeDealCommandUseCase useCase;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        useCase = mock(TimeDealCommandUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new TimeDealController(useCase)).build();
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
                    .andExpect(jsonPath("$.traceId").value(nullValue()));
        }
    }
}
