package com.parut.product.timedeal.application.initializer;

import com.parut.product.timedeal.application.port.out.timedeal.TimeDealRepository;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealScheduleEntry;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealScheduleRedisPort;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Redis가 재시작되거나 초기화되어도 DB의 판매 기간을 기준으로 예약 큐를 복구한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TimeDealSalePeriodScheduleInitializer {

    private static final int PAGE_SIZE = 500;

    private final TimeDealRepository timeDealRepository;
    private final TimeDealScheduleRedisPort scheduleRedisPort;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeAfterApplicationReady() {
        reconcile();
    }

    public void reconcile() {
        UUID lastSeenId = null;
        int restoredCount = 0;

        try {
            while (true) {
                List<TimeDealScheduleEntry> schedules =
                        timeDealRepository.findSalePeriodSchedulesAfter(lastSeenId, PAGE_SIZE);

                for (TimeDealScheduleEntry schedule : schedules) {
                    scheduleRedisPort.schedule(
                            schedule.timeDealId(), schedule.startAt(), schedule.endAt());
                }

                restoredCount += schedules.size();
                if (schedules.size() < PAGE_SIZE) {
                    break;
                }
                lastSeenId = schedules.get(schedules.size() - 1).timeDealId();
            }

            log.info("[TimeDealRedis] 판매 기간 예약 복구 완료. restoredCount={}", restoredCount);
        } catch (RuntimeException exception) {
            // Redis 또는 DB의 일시 장애로 애플리케이션 기동 자체가 중단되지는 않도록 한다.
            log.error("[TimeDealRedis] 서버 시작 시 판매 기간 예약 복구 실패. restoredCount={}",
                    restoredCount, exception);
        }
    }
}
