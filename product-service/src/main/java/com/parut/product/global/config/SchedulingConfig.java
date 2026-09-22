package com.parut.product.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulingConfig {
    // 별도 스케줄러 등록 시 Boot 자동 설정이 물러나므로 기존 작업용 기본 빈도 명시한다.
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        return scheduler("scheduled-", 1);
    }

    @Bean
    public ThreadPoolTaskScheduler timeDealTaskScheduler() {
        return scheduler("time-deal-sale-period-", 2);
    }

    @Bean
    public ThreadPoolTaskScheduler timeDealPurchaseExpirationTaskScheduler() {
        return scheduler("time-deal-purchase-expiration-", 1);
    }

    @Bean
    public ThreadPoolTaskScheduler timeDealOpeningSoonTaskScheduler() {
        return scheduler("time-deal-opening-soon-", 1);
    }

    private ThreadPoolTaskScheduler scheduler(String prefix, int poolSize) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix(prefix);
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }
}
