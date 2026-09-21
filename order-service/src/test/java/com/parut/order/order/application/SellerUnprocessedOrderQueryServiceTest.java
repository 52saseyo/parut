package com.parut.order.order.application;

import com.parut.order.order.domain.DeliveryGroupStatus;
import com.parut.order.order.domain.OrderItemStatus;
import com.parut.order.order.infrastructure.persistence.OrderDeliveryGroupRepository;
import com.parut.order.order.infrastructure.persistence.OrderItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerUnprocessedOrderQueryServiceTest {

    private static final UUID SELLER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b3");

    @Mock
    private OrderDeliveryGroupRepository orderDeliveryGroupRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private SellerUnprocessedOrderQueryService sellerUnprocessedOrderQueryService;

    @Test
    @DisplayName("발송 대기(PREPARING) 배송그룹이 있으면 미처리 주문이 있다고 판단한다")
    void 발송_대기_그룹_존재() {
        when(orderDeliveryGroupRepository.existsBySellerIdAndGroupStatus(SELLER_ID, DeliveryGroupStatus.PREPARING))
                .thenReturn(true);

        boolean result = sellerUnprocessedOrderQueryService.hasUnprocessedOrder(SELLER_ID);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("환불 승인 대기(REFUND_REQUESTED) 아이템이 있으면 미처리 주문이 있다고 판단한다")
    void 환불_승인_대기_아이템_존재() {
        when(orderDeliveryGroupRepository.existsBySellerIdAndGroupStatus(SELLER_ID, DeliveryGroupStatus.PREPARING))
                .thenReturn(false);
        when(orderItemRepository.existsBySellerIdAndItemStatus(SELLER_ID, OrderItemStatus.REFUND_REQUESTED))
                .thenReturn(true);

        boolean result = sellerUnprocessedOrderQueryService.hasUnprocessedOrder(SELLER_ID);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("두 조건 모두 해당 없으면 미처리 주문이 없다고 판단한다")
    void 미처리_주문_없음() {
        when(orderDeliveryGroupRepository.existsBySellerIdAndGroupStatus(SELLER_ID, DeliveryGroupStatus.PREPARING))
                .thenReturn(false);
        when(orderItemRepository.existsBySellerIdAndItemStatus(SELLER_ID, OrderItemStatus.REFUND_REQUESTED))
                .thenReturn(false);

        boolean result = sellerUnprocessedOrderQueryService.hasUnprocessedOrder(SELLER_ID);

        assertThat(result).isFalse();
    }
}
