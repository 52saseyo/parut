package com.parut.product.timedeal.infrastructure.client;

import com.parut.product.global.dto.ProductStockAllocateCommand;
import com.parut.product.global.dto.ProductStockAllocateResult;
import com.parut.product.product.application.stock.service.ProductStockService;
import com.parut.product.timedeal.application.port.out.product.ProductStockAllocationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// NOTE: 지금은 같은 jar 안의 in-process 호출이다. product가 별도 서비스로 갈리면 이 클래스만 Feign 호출로 바꾼다.
@Component
@RequiredArgsConstructor
public class ProductStockAllocationAdapter implements ProductStockAllocationPort {

    private final ProductStockService productStockService;

    // TODO: ProductStockService.allocate(ProductStockAllocateCommand)가 생기면
    //  return productStockService.allocate(productStockAllocateCommand); 로 교체한다.
    // NOTE: 값이 빈 결과를 돌려주면 호출부에서 "상품 품질이 필수입니다"(400) 같은 엉뚱한 사유로 실패해
    // 원인이 가려지므로, 연결 전까지는 미구현임을 그대로 드러낸다.
    @Override
    public ProductStockAllocateResult allocate(ProductStockAllocateCommand productStockAllocateCommand) {
        return productStockService.allocate(productStockAllocateCommand);
    }
}