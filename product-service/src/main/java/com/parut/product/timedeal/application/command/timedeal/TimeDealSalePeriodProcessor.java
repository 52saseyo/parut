package com.parut.product.timedeal.application.command.timedeal;

import com.parut.product.timedeal.application.port.out.timedeal.TimeDealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TimeDealSalePeriodProcessor {
    private final TimeDealRepository timeDealRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 5)
    public void synchronize(UUID timeDealId) {
        timeDealRepository.findByIdForUpdate(timeDealId).ifPresent(timeDeal -> {
            // 잠금 대기 이후의 시각으로 판단해야 이미 끝난 타임딜을 활성화하지 않는다.
            Instant now = Instant.now();
            timeDeal.updateSaleStatus(now);
        });
    }
}
