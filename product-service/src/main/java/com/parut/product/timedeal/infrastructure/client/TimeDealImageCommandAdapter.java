package com.parut.product.timedeal.infrastructure.client;

import com.parut.product.global.dto.TimeDealImageSaveCommand;
import com.parut.product.timedeal.application.port.out.image.TimeDealImageCommandPort;
import com.parut.product.image.application.service.TimeDealImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TimeDealImageCommandAdapter implements TimeDealImageCommandPort {

    private final TimeDealImageService timeDealImageService;

    @Override
    public void save(TimeDealImageSaveCommand command) {
        timeDealImageService.copyFromProductImage(command.timeDealId(), command.imageId());
    }
}
