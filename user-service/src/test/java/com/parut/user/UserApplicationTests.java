package com.parut.user;

import com.parut.user.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("integration-test")
class UserApplicationTests extends PostgresIntegrationTest {

    @Test
    void contextLoads() {
    }

}
