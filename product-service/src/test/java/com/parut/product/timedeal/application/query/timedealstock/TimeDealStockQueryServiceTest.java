package com.parut.product.timedeal.application.query.timedealstock;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealRepository;
import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockRepository;
import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import com.parut.product.timedeal.domain.timedealstock.TimeDealStock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeDealStockQueryServiceTest {

    private static final UUID TIME_DEAL_ID = UUID.randomUUID();

    @Mock
    private TimeDealRepository timeDealRepository;

    @Mock
    private TimeDealStockRepository timeDealStockRepository;

    private TimeDealStockQueryService service;

    @BeforeEach
    void setUp() {
        service = new TimeDealStockQueryService(timeDealRepository, timeDealStockRepository);
    }

    @Test
    void 타임딜과_재고를_조회해_재고_결과를_반환한다() {
        when(timeDealRepository.findById(TIME_DEAL_ID)).thenReturn(Optional.of(org.mockito.Mockito.mock(TimeDeal.class)));
        when(timeDealStockRepository.findByTimeDealId(TIME_DEAL_ID))
                .thenReturn(Optional.of(TimeDealStock.create(TIME_DEAL_ID, 90, 10)));

        var result = service.getStock(TIME_DEAL_ID);

        assertThat(result.timeDealId()).isEqualTo(TIME_DEAL_ID);
        assertThat(result.availableQuantity()).isEqualTo(90);
        assertThat(result.reservedQuantity()).isZero();
        assertThat(result.soldQuantity()).isZero();
        assertThat(result.lowStockThreshold()).isEqualTo(10);
    }

    @Test
    void 타임딜이_없으면_404_예외를_반환한다() {
        when(timeDealRepository.findById(TIME_DEAL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStock(TIME_DEAL_ID))
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TIME_DEAL_NOT_FOUND);

        verify(timeDealStockRepository, org.mockito.Mockito.never()).findByTimeDealId(TIME_DEAL_ID);
    }

    @Test
    void 재고가_없으면_404_예외를_반환한다() {
        when(timeDealRepository.findById(TIME_DEAL_ID)).thenReturn(Optional.of(org.mockito.Mockito.mock(TimeDeal.class)));
        when(timeDealStockRepository.findByTimeDealId(TIME_DEAL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStock(TIME_DEAL_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TIME_DEAL_STOCK_NOT_FOUND);
    }
}
