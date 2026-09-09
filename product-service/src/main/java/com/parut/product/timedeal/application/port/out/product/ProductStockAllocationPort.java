package com.parut.product.timedeal.application.port.out.product;

import com.parut.product.global.dto.ProductStockAllocateCommand;
import com.parut.product.global.dto.ProductStockAllocateResult;

// NOTE: 일반 상품 재고를 타임딜 재고로 넘겨받는 아웃바운드 포트.
// application이 product 모듈을 직접 알지 않도록 계약만 선언하고, 구현은 infrastructure의 어댑터가 맡는다.
// NOTE: 애그리거트 포트가 아니라 다른 컨텍스트를 향한 의존이라 port/out/product/에 둔다.
public interface ProductStockAllocationPort {

    ProductStockAllocateResult allocate(ProductStockAllocateCommand productStockAllocateCommand);
}