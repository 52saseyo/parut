package com.parut.product.timedeal.infrastructure.client;

import com.parut.product.global.dto.ImageQuery;
import com.parut.product.global.dto.ImageQueryResult;
import com.parut.product.timedeal.application.port.out.image.ImageQueryPort;
import com.parut.product.image.application.service.TimeDealImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageQueryAdapter implements ImageQueryPort {

    private final TimeDealImageService timeDealImageService;

    @Override
    public ImageQueryResult findImage(ImageQuery query) {
        return timeDealImageService.getImageInfo(query.timeDealId())
                .orElse(new ImageQueryResult(null, null));
    }
}
