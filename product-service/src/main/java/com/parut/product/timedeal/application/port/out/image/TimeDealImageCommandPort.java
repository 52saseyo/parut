package com.parut.product.timedeal.application.port.out.image;

import com.parut.product.global.dto.TimeDealImageSaveCommand;

public interface TimeDealImageCommandPort {

    void save(TimeDealImageSaveCommand command);
}
