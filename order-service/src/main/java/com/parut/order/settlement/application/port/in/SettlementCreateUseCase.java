package com.parut.order.settlement.application.port.in;

import java.util.UUID;

public interface SettlementCreateUseCase {

    void createSettlement(UUID orderItemId);
}
