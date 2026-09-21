package com.parut.product.timedeal.presentation.scheduler;

import com.parut.product.timedeal.application.port.in.timedeal.TimeDealCommandUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TimeDealSalePeriodScheduler {
    private final TimeDealCommandUseCase timeDealCommandUseCase;

    @Scheduled(fixedDelayString = "${parut.time-deal.sale-period-interval:1m}",
            scheduler = "timeDealTaskScheduler")
    public void endTimeDeals() {
        timeDealCommandUseCase.endTimeDeals();
    }
    @Scheduled(fixedDelayString = "${parut.time-deal.sale-period-interval:1m}",
            scheduler = "timeDealTaskScheduler")
    public void activateTimeDeals() {
        timeDealCommandUseCase.activateTimeDeals();
    }

}
