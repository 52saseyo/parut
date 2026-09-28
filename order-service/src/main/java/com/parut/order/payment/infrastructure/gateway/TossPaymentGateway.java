package com.parut.order.payment.infrastructure.gateway;

import com.parut.order.payment.application.port.out.PaymentGateway;
import com.parut.order.payment.application.port.out.dto.PaymentApproveResult;
import com.parut.order.payment.application.port.out.dto.PaymentCancelResult;
import com.parut.order.payment.domain.PaymentMethod;
import com.parut.order.payment.infrastructure.gateway.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;

@Slf4j
@Component
@ConditionalOnProperty(name = "toss.enabled", havingValue = "true", matchIfMissing = true)
public class TossPaymentGateway implements PaymentGateway {

    private final RestClient tossRestClient;

    public TossPaymentGateway(
            @Value("${toss.secret-key}") String secretKey,
            @Value("${toss.base-url}") String baseUrl
    ) {
        String encodedAuth = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        this.tossRestClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth)
                .build();
    }

    @Override
    public void ready(String tossOrderId, long amount) {
        // Toss는 결제창 오픈 전 별도 서버 간 준비 호출이 없다 (결제창 자체가 세션 역할)
    }

    @Override
    public PaymentApproveResult approve(String paymentKey, String tossOrderId, long amount, String idempotencyKey) {
        TossPaymentResponse response;
        try {
            response = tossRestClient.post()
                    .uri("/v1/payments/confirm")
                    .header("Idempotency-Key", idempotencyKey)
                    .body(new TossConfirmRequest(paymentKey, tossOrderId, amount))
                    .retrieve()
                    .body(TossPaymentResponse.class);
        } catch (RestClientResponseException e) {
            TossErrorResponse error = e.getResponseBodyAs(TossErrorResponse.class);
            log.warn("[TossPaymentGateway] 결제 승인 실패. tossOrderId={}, status={}, code={}, message={}",
                    tossOrderId, e.getStatusCode(),
                    error != null ? error.code() : null,
                    error != null ? error.message() : e.getMessage());
            throw e;
        } catch (RestClientException e) {
            log.warn("[TossPaymentGateway] 결제 승인 요청 실패(네트워크/응답 오류). tossOrderId={}", tossOrderId, e);
            throw e;
        }

        return new PaymentApproveResult(
                mapPaymentMethod(response.method()),
                OffsetDateTime.parse(response.approvedAt()).toInstant(),
                response.receipt().url(),
                response.lastTransactionKey()
        );
    }

    @Override
    public PaymentCancelResult cancel(String paymentKey, long cancelAmount, String reason) {
        TossCancelResponse response;
        try {
            response = tossRestClient.post()
                    .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                    .body(new TossCancelRequest(reason, cancelAmount))
                    .retrieve()
                    .body(TossCancelResponse.class);
        } catch (RestClientResponseException e) {
            TossErrorResponse error = e.getResponseBodyAs(TossErrorResponse.class);
            log.warn("[TossPaymentGateway] 결제 취소 실패. paymentKey={}, status={}, code={}, message={}",
                    paymentKey, e.getStatusCode(),
                    error != null ? error.code() : null,
                    error != null ? error.message() : e.getMessage());
            throw e;
        } catch (RestClientException e) {
            log.warn("[TossPaymentGateway] 결제 취소 요청 실패(네트워크/응답 오류). paymentKey={}", paymentKey, e);
            throw e;
        }

        TossCancelResponse.Cancel lastCancel = response.cancels().getLast();
        return new PaymentCancelResult(
                OffsetDateTime.parse(lastCancel.canceledAt()).toInstant(),
                lastCancel.transactionKey()
        );
    }

    @Override
    public String getStatus(String paymentKey) {
        TossPaymentStatusResponse response;
        try {
            response = tossRestClient.get()
                    .uri("/v1/payments/{paymentKey}", paymentKey)
                    .retrieve()
                    .body(TossPaymentStatusResponse.class);
        } catch (RestClientResponseException e) {
            TossErrorResponse error = e.getResponseBodyAs(TossErrorResponse.class);
            log.warn("[TossPaymentGateway] 결제 조회 실패. paymentKey={}, status={}, code={}, message={}",
                    paymentKey, e.getStatusCode(),
                    error != null ? error.code() : null,
                    error != null ? error.message() : e.getMessage());
            throw e;
        } catch (RestClientException e) {
            log.warn("[TossPaymentGateway] 결제 조회 요청 실패(네트워크/응답 오류). paymentKey={}", paymentKey, e);
            throw e;
        }

        return response.status();
    }

    private PaymentMethod mapPaymentMethod(String tossMethod) {
        // PaymentMethod가 CREDIT_CARD/TOSS_PAY 2종뿐이라 단순 매핑한다.
        // 가상계좌·계좌이체 등 다른 결제수단을 열려면 enum 확장이 먼저 필요하다.
        return "카드".equals(tossMethod) ? PaymentMethod.CREDIT_CARD : PaymentMethod.TOSS_PAY;
    }
}
