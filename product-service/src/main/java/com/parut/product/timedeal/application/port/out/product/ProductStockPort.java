package com.parut.product.timedeal.application.port.out.product;

import com.parut.product.global.dto.ProductStockAllocateCommand;
import com.parut.product.global.dto.ProductStockAllocateResult;
import com.parut.product.global.dto.ProductStockTransferCommand;
import com.parut.product.global.dto.ProductStockTransferResult;

// NOTE: 타임딜 컨텍스트가 일반 상품 재고 기능을 호출하기 위한 공통 아웃바운드 포트다.
public interface ProductStockPort {

    ProductStockAllocateResult allocate(ProductStockAllocateCommand productStockAllocateCommand);

    ProductStockTransferResult transfer(ProductStockTransferCommand productStockTransferCommand);
}
