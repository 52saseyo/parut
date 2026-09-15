package com.parut.order.payment.application.port.in;

import com.parut.order.payment.application.port.in.dto.PaymentCancelCommand;
import com.parut.order.payment.application.port.in.dto.PaymentCancelView;

/**
 * 주문 환불과 주문 취소에서 사용할 결제 부분 취소 계약을 제공한다.
 */
public interface PaymentCancelUseCase {

    PaymentCancelView cancel(PaymentCancelCommand command);
}
