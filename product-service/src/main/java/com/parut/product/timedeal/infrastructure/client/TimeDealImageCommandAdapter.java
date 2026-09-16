package com.parut.product.timedeal.infrastructure.client;

import com.parut.product.global.dto.TimeDealImageSaveCommand;
import com.parut.product.timedeal.application.port.out.image.TimeDealImageCommandPort;
import org.springframework.stereotype.Component;

@Component
public class TimeDealImageCommandAdapter implements TimeDealImageCommandPort {

    @Override
    public void save(TimeDealImageSaveCommand command) {
        // TODO: timedeal_image 테이블과 Image Service 저장 계약이 생기면 구현한다.
       return;
    }
}
