package com.parut.product;

import com.parut.product.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("integration-test")
class ProductApplicationTests extends PostgresIntegrationTest {

	@Test
	void contextLoads() {
	}

}
