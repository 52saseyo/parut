package com.parut.product.timedeal.presentation.scheduler;

import com.parut.product.timedeal.application.port.in.timedealpurchase.TimeDealPurchaseCommandUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TimeDealPurchaseExpirationScheduler {

    private final TimeDealPurchaseCommandUseCase timeDealPurchaseCommandUseCase;

    @Scheduled(fixedDelayString = "${parut.time-deal.purchase-expiration-interval:1m}",
            scheduler = "timeDealPurchaseExpirationTaskScheduler")
    public void expireReservations() {
        timeDealPurchaseCommandUseCase.expireReservations();
    }
}
