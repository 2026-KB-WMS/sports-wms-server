package com.kb.wms.inbound.adapter.out.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.kb.wms.inbound.application.port.out.LotPort;
import com.kb.wms.inventory.application.port.in.LotUseCase;
import com.kb.wms.inventory.application.port.in.command.LotRegisterCommand;

import lombok.RequiredArgsConstructor;

/**
 * 입고 도메인의 로트 포트를 재고 도메인 유스케이스에 연결한다.
 */
@Component
@RequiredArgsConstructor
public class LotAdapter implements LotPort {

    private final LotUseCase lotUseCase;

    @Override
    public Long findOrRegisterLot(Long skuId, Long supplierId, String lotNumber,
                                  LocalDate manufacturedDate, LocalDate expiryDate, BigDecimal unitCost) {
        return lotUseCase.findOrRegister(
                new LotRegisterCommand(skuId, supplierId, lotNumber, manufacturedDate, expiryDate, unitCost))
                .getLotId();
    }
}
