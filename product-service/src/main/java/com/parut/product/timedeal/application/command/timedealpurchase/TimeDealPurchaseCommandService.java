package com.parut.product.timedeal.application.command.timedealpurchase;

import com.parut.product.timedeal.application.dto.timedealpurchase.TimeDealPurchaseCancelCommand;
import com.parut.product.timedeal.application.dto.timedealpurchase.TimeDealPurchaseConfirmCommand;
import com.parut.product.timedeal.application.dto.timedealpurchase.TimeDealPurchaseReserveCommand;
import com.parut.product.timedeal.application.port.in.timedealpurchase.TimeDealPurchaseCommandUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class TimeDealPurchaseCommandService implements TimeDealPurchaseCommandUseCase {
 // TODO:  아웃바운드 포트(TimeDealRepository / TimeDealStockRepository / TimeDealPurchaseRepository)와 TimeDealPolicy 도메인 서비스를 주입받아 구현한다.
    @Override
    public void reserve(TimeDealPurchaseReserveCommand command) {

    }

    @Override
    public void confirm(TimeDealPurchaseConfirmCommand command) {

    }

    @Override
    public void cancel(TimeDealPurchaseCancelCommand command) {

    }
}