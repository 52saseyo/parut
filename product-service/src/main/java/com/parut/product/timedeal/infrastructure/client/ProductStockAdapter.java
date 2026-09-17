package com.parut.product.timedeal.infrastructure.client;

import com.parut.product.global.dto.ProductStockAllocateCommand;
import com.parut.product.global.dto.ProductStockAllocateResult;
import com.parut.product.global.dto.ProductStockTransferCommand;
import com.parut.product.global.dto.ProductStockTransferResult;
import com.parut.product.product.application.stock.service.ProductStockService;
import com.parut.product.timedeal.application.port.out.product.ProductStockPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// NOTE: 지금은 같은 jar 안의 in-process 호출이다. Product가 별도 서비스로 갈리면 이 클래스만 Feign 호출로 바꾼다.
@Component
@RequiredArgsConstructor
public class ProductStockAdapter implements ProductStockPort {

    private final ProductStockService productStockService;

    @Override
    public ProductStockAllocateResult allocate(ProductStockAllocateCommand productStockAllocateCommand) {
        return productStockService.allocate(productStockAllocateCommand);
    }

    @Override
    public ProductStockTransferResult transfer(ProductStockTransferCommand productStockTransferCommand) {
        return productStockService.transferStock(productStockTransferCommand);
    }
}
