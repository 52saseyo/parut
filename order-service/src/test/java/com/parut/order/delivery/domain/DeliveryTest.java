package com.parut.order.delivery.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DeliveryTest {

    private static final UUID DELIVERY_GROUP_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b8");
    private static final Instant SHIPPED_AT = Instant.parse("2026-09-05T01:00:00Z");
    private static final Instant DELIVERED_AT = Instant.parse("2026-09-05T07:00:00Z");

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("배송 준비 상태로 생성된다")
        void 생성_성공() {
            Delivery delivery = Delivery.create(DELIVERY_GROUP_ID);

            assertThat(delivery.getDeliveryGroupId()).isEqualTo(DELIVERY_GROUP_ID);
            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.PREPARING);
        }

        @Test
        @DisplayName("배송 그룹 ID가 없으면 생성할 수 없다")
        void 배송_그룹_ID_필수() {
            assertThatThrownBy(() -> Delivery.create(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("배송 그룹 ID는 필수입니다.");
        }
    }

    @Nested
    @DisplayName("배송 시작")
    class Ship {

        @Test
        @DisplayName("운송장을 등록하면 배송 중 상태가 된다")
        void 배송_시작_성공() {
            Delivery delivery = delivery();
            String trackingNumber = "1".repeat(30);

            delivery.ship(trackingNumber, SHIPPED_AT);

            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.SHIPPED);
            assertThat(delivery.getTrackingNumber()).isEqualTo(trackingNumber);
            assertThat(delivery.getShippedAt()).isEqualTo(SHIPPED_AT);
        }

        @Test
        @DisplayName("운송장 번호가 비어 있거나 30자를 초과하면 배송을 시작할 수 없다")
        void 운송장_번호_검증() {
            assertThatThrownBy(() -> delivery().ship(" ", SHIPPED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("운송장 번호는 필수입니다.");
            assertThatThrownBy(() -> delivery().ship("1".repeat(31), SHIPPED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("운송장 번호는 30자를 초과할 수 없습니다.");
        }

        @Test
        @DisplayName("배송 준비 상태가 아니면 배송을 시작할 수 없다")
        void 배송_시작_상태_검증() {
            Delivery delivery = delivery();
            delivery.ship("1234567890", SHIPPED_AT);

            assertThatThrownBy(() -> delivery.ship("0987654321", SHIPPED_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("배송 준비 상태에서만 배송을 시작할 수 있습니다.");
        }
    }

    @Nested
    @DisplayName("배송 완료")
    class Complete {

        @Test
        @DisplayName("배송 중인 배송을 완료한다")
        void 배송_완료_성공() {
            Delivery delivery = shippedDelivery();

            delivery.complete(DELIVERED_AT);

            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
            assertThat(delivery.getDeliveredAt()).isEqualTo(DELIVERED_AT);
        }

        @Test
        @DisplayName("배송 중이 아니거나 완료 시각이 시작 시각보다 빠르면 완료할 수 없다")
        void 배송_완료_조건_검증() {
            assertThatThrownBy(() -> delivery().complete(DELIVERED_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("배송 중 상태에서만 배송을 완료할 수 있습니다.");
            assertThatThrownBy(() -> shippedDelivery().complete(SHIPPED_AT.minusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("배송 완료 시각은 배송 시작 시각보다 빠를 수 없습니다.");
        }
    }

    private Delivery delivery() {
        return Delivery.create(DELIVERY_GROUP_ID);
    }

    private Delivery shippedDelivery() {
        Delivery delivery = delivery();
        delivery.ship("1234567890", SHIPPED_AT);
        return delivery;
    }
}
