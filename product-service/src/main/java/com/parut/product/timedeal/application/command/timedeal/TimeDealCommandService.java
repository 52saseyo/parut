package com.parut.product.timedeal.application.command.timedeal;

import com.parut.product.timedeal.application.dto.timedeal.TimeDealCreateCommand;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealCreateResult;
import com.parut.product.timedeal.application.port.in.timedeal.TimeDealCommandUseCase;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealRepository;
import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockRepository;
import com.parut.product.timedeal.domain.common.TimeDealPolicy;
import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import com.parut.product.timedeal.domain.timedealstock.TimeDealStock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class TimeDealCommandService implements TimeDealCommandUseCase {

    private final TimeDealRepository timeDealRepository;
    private final TimeDealStockRepository timeDealStockRepository;
    private final TimeDealPolicy timeDealPolicy;


    // NOTE: 직접 등록이라 productId는 null이다 — 표시 정보는 판매자가 입력한 값이 그대로 스냅샷이 된다.
    @Override
    @Transactional
    public TimeDealCreateResult create(TimeDealCreateCommand timeDealCreateCommand) {
        // NOTE: now는 유즈케이스당 한 번만 만들어 모든 도메인 호출에 같은 값을 넘긴다.
        Instant now = Instant.now();

        TimeDeal timeDeal = TimeDeal.create(
                timeDealCreateCommand.sellerId(),
                null,
                timeDealCreateCommand.imageId(),
                timeDealCreateCommand.name(),
                timeDealCreateCommand.description(),
                timeDealCreateCommand.productGrade(),
                timeDealCreateCommand.origin(),
                timeDealCreateCommand.harvestedAt(),
                timeDealCreateCommand.originalPrice(),
                timeDealCreateCommand.discountRate(),
                timeDealCreateCommand.startAt(),
                timeDealCreateCommand.endAt(),
                timeDealCreateCommand.maxPurchaseQuantity(),
                now
        );

        // NOTE: 재고는 타임딜 ID로 참조하므로 먼저 저장해 ID를 확보해야 한다(저장 전에는 null).
        TimeDeal savedTimeDeal = timeDealRepository.save(timeDeal);

        // NOTE: maxPurchaseQuantity <= 초기 재고 검증이 여기 있는 이유는 두 값이 다른 애그리거트에 있기 때문이다.
        TimeDealStock timeDealStock = timeDealPolicy.allocateStock(
                savedTimeDeal,
                timeDealCreateCommand.initialQuantity(),
                timeDealCreateCommand.lowStockThreshold()
        );
        timeDealStockRepository.save(timeDealStock);

        log.info(
                "[TimeDeal] 직접 등록 완료. timeDealId={}, sellerId={}, initialQuantity={}",
                savedTimeDeal.getId(),
                timeDealCreateCommand.sellerId(),
                timeDealCreateCommand.initialQuantity()
        );
        return TimeDealCreateResult.from(savedTimeDeal);
    }
}
