package com.parut.product.timedeal.application.port.out.image;

import com.parut.product.global.dto.ImageQuery;
import com.parut.product.global.dto.ImageQueryResult;

import java.util.UUID;

public interface ImageQueryPort {

    ImageQueryResult findImage(ImageQuery query);
}
