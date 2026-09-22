package com.parut.product.timedeal.presentation.scheduler;

import com.parut.product.timedeal.application.port.in.timedealpurchase.TimeDealPurchaseCommandUseCase;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TimeDealPurchaseExpirationSchedulerTest {

    @Test
    void 만료_스케줄러가_구매_선점_만료_유즈케이스를_호출한다() {
        TimeDealPurchaseCommandUseCase timeDealPurchaseCommandUseCase = mock(TimeDealPurchaseCommandUseCase.class);
        TimeDealPurchaseExpirationScheduler scheduler =
                new TimeDealPurchaseExpirationScheduler(timeDealPurchaseCommandUseCase);

        scheduler.expireReservations();

        verify(timeDealPurchaseCommandUseCase).expireReservations();
    }
}
