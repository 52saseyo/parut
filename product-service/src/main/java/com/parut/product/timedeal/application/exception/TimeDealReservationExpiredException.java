package com.parut.product.timedeal.application.exception;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;

// NOTE: 전용 타입이 필요한 이유는 noRollbackFor가 ErrorCode가 아니라 예외 타입만 보기 때문이다.
// BusinessException으로 넓게 지정하면 같은 메서드의 다른 실패까지 커밋되어 부분 쓰기가 남는다.
public class TimeDealReservationExpiredException extends BusinessException {

    public TimeDealReservationExpiredException() {
        super(ErrorCode.TIME_DEAL_RESERVATION_EXPIRED);
    }
}
