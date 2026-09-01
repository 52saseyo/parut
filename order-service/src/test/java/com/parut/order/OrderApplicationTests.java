package com.parut.order;

import com.parut.order.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("integration-test")
class OrderApplicationTests extends PostgresIntegrationTest {

    @Test
    void contextLoads() {
    }

}
