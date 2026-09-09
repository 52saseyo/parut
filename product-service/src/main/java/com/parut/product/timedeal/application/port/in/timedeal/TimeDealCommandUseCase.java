package com.parut.product.timedeal.application.port.in.timedeal;

import com.parut.product.timedeal.application.dto.timedeal.TimeDealCreateCommand;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealCreateResult;

public interface TimeDealCommandUseCase {

    // NOTE: 타임딜과 재고를 함께 만들고 생성된 타임딜의 식별자·상태·기간을 돌려준다.
    TimeDealCreateResult create(TimeDealCreateCommand timeDealCreateCommand);
}
