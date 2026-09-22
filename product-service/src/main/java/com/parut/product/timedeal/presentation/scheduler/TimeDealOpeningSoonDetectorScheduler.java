package com.parut.product.timedeal.presentation.scheduler;

import com.parut.product.timedeal.application.command.timedeal.TimeDealOpeningSoonDetector;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 시작 임박 타임딜을 주기적으로 감지해 Outbox에 적재하도록 요청한다.
 * 실제 이벤트 중복 방지는 Detector와 Outbox의 유니크 제약이 담당한다.
 */
@Component
@RequiredArgsConstructor
public class TimeDealOpeningSoonDetectorScheduler {

    private final TimeDealOpeningSoonDetector detector;

    @Scheduled(
            fixedDelayString = "${parut.time-deal.opening-soon-interval:1m}",
            scheduler = "timeDealOpeningSoonTaskScheduler"
    )
    public void detectOpeningSoonTimeDeals() {
        detector.detect(Instant.now());
    }
}
