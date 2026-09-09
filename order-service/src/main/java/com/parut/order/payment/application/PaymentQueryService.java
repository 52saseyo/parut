package com.parut.order.payment.application;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parut.order.payment.application.port.in.PaymentQueryUseCase;
import com.parut.order.payment.application.port.in.dto.PaymentView;
import com.parut.order.payment.infrastructure.persistence.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentQueryService implements PaymentQueryUseCase {

    private final PaymentRepository paymentRepository;

    @Override
    public Optional<PaymentView> getPayment(UUID orderId) {
        return paymentRepository.findByOrderId(orderId).map(PaymentView::from);
    }
}
