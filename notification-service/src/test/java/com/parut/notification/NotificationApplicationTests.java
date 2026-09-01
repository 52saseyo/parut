package com.parut.notification;

import com.parut.notification.support.PostgresIntegrationTest;
import com.parut.notification.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("integration-test")
class NotificationApplicationTests extends PostgresIntegrationTest {

	@Test
	void contextLoads() {
	}

}
