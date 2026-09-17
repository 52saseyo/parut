package com.parut.product.timedeal.infrastructure.client;

import com.parut.product.global.dto.ImageQuery;
import com.parut.product.global.dto.ImageQueryResult;
import com.parut.product.timedeal.application.port.out.image.ImageQueryPort;
import org.springframework.stereotype.Component;

@Component
public class ImageQueryAdapter implements ImageQueryPort {

    @Override
    public ImageQueryResult findImage(ImageQuery query) {
        // TODO: Image Service가 구현되면 timeDealId로 대표 이미지 정보를 조회한다.
        return new ImageQueryResult(null, null);
    }
}
