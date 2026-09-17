package com.parut.product.timedeal.application.command.timedealstock;

import com.parut.product.global.dto.ProductStockTransferCommand;
import com.parut.product.global.dto.ProductStockTransferResult;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.timedeal.application.authorization.TimeDealAuthorizationChecker;
import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockTransferCommand;
import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockTransferResult;
import com.parut.product.timedeal.application.port.out.product.ProductStockPort;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealRepository;
import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockRepository;
import com.parut.product.timedeal.domain.common.TimeDealPolicy;
import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import com.parut.product.timedeal.domain.timedeal.TimeDealProductGrade;
import com.parut.product.timedeal.domain.timedealstock.TimeDealStock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeDealStockCommandServiceTest {

    private static final UUID TIME_DEAL_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-09-01T00:00:00Z");

    @Mock
    private TimeDealRepository timeDealRepository;

    @Mock
    private ProductStockPort productStockPort;

    @Mock
    private TimeDealStockRepository timeDealStockRepository;

    private TimeDealStockCommandService service;

    @BeforeEach
    void setUp() {
        service = new TimeDealStockCommandService(
                timeDealRepository,
                productStockPort,
                timeDealStockRepository,
                new TimeDealPolicy(),
                new TimeDealAuthorizationChecker()
        );
    }

    @Test
    void 상품_재고_이동_응답의_실제수량으로_타임딜_재고를_변경한다() {
        TimeDeal timeDeal = existingTimeDeal();
        TimeDealStock timeDealStock = TimeDealStock.create(TIME_DEAL_ID, 50, 5);
        when(timeDealStockRepository.findByTimeDealId(TIME_DEAL_ID)).thenReturn(Optional.of(timeDealStock));

        ProductStockTransferResult productResult =
                ProductStockTransferResult.of(PRODUCT_ID, -20, 80);
        when(productStockPort.transfer(org.mockito.ArgumentMatchers.any()))
                .thenReturn(productResult);

        TimeDealStockTransferResult result = service.transferStock(
                new TimeDealStockTransferCommand(TIME_DEAL_ID, PRODUCT_ID, 20, SELLER_ID, "SELLER")
        );

        assertThat(timeDealStock.getAvailableQuantity()).isEqualTo(70);
        assertThat(result.quantity()).isEqualTo(20);
        assertThat(result.productAvailableQuantity()).isEqualTo(80);

        ArgumentCaptor<ProductStockTransferCommand> captor =
                ArgumentCaptor.forClass(ProductStockTransferCommand.class);
        verify(productStockPort).transfer(captor.capture());
        assertThat(captor.getValue().quantity()).isEqualTo(-20);
    }

    @Test
    void 연결된_상품이_아니면_상품_재고_이동을_호출하지_않는다() {
        TimeDeal timeDeal = existingTimeDeal();
        when(timeDealRepository.findById(TIME_DEAL_ID)).thenReturn(Optional.of(timeDeal));

        assertThatThrownBy(() -> service.transferStock(
                new TimeDealStockTransferCommand(TIME_DEAL_ID, UUID.randomUUID(), 20, SELLER_ID, "SELLER")
        ))
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TIME_DEAL_STOCK_TRANSFER_NOT_ALLOWED);

        verifyNoInteractions(productStockPort, timeDealStockRepository);
    }

    private TimeDeal existingTimeDeal() {
        TimeDeal timeDeal = TimeDeal.create(
                SELLER_ID,
                PRODUCT_ID,
                "사과",
                "설명",
                TimeDealProductGrade.UGLY,
                "안동",
                LocalDate.of(2026, 9, 1),
                10_000L,
                BigDecimal.valueOf(30),
                NOW.plusSeconds(3600),
                NOW.plusSeconds(7200),
                10,
                NOW
        );
        ReflectionTestUtils.setField(timeDeal, "id", TIME_DEAL_ID);
        when(timeDealRepository.findById(TIME_DEAL_ID)).thenReturn(Optional.of(timeDeal));
        return timeDeal;
    }
}
