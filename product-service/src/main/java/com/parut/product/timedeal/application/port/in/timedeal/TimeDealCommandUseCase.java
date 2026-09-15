package com.parut.product.timedeal.application.port.in.timedeal;

import com.parut.product.timedeal.application.dto.timedeal.TimeDealConvertCommand;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealCreateCommand;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealCreateResult;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealUpdateCommand;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealUpdateResult;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealDeleteCommand;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealStopCommand;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealStopResult;

public interface TimeDealCommandUseCase {

    void endTimeDeals();

    void activateTimeDeals();

    void delete(TimeDealDeleteCommand command);

    TimeDealStopResult stop(TimeDealStopCommand command);

    TimeDealUpdateResult update(TimeDealUpdateCommand command);

    // NOTE: 판매자 직접 등록. 타임딜과 재고를 함께 만들고 생성된 타임딜의 식별자·상태·기간을 돌려준다.
    TimeDealCreateResult create(TimeDealCreateCommand timeDealCreateCommand);

    // NOTE: 일반 상품 전환. 일반 재고를 할당받아 타임딜을 만든다 — 전환으로 만든 타임딜도 독립된 타임딜이며,
    // 이후 남은 재고를 일반 상품으로 되돌리는 흐름은 없다.
    TimeDealCreateResult convert(TimeDealConvertCommand timeDealConvertCommand);
}
