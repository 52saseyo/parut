package com.parut.order.global.common;

import org.springframework.data.domain.Sort;

public enum SortDirection {
    ASC,
    DESC;

    public Sort.Direction toSpringDirection() {
        return this == ASC
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
    }
}
