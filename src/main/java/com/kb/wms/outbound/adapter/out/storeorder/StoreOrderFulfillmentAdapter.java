package com.kb.wms.outbound.adapter.out.storeorder;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.kb.wms.outbound.application.port.out.StoreOrderPort;
import com.kb.wms.outbound.domain.enums.StoreOrderState;
import com.kb.wms.storeorder.application.port.in.StoreOrderFulfillmentUseCase;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderLinePickedCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderLineQuantityCommand;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderFulfillmentLineView;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderFulfillmentOrderView;

import lombok.RequiredArgsConstructor;

/**
 * 출고가 지점 발주를 다루는 포트({@link StoreOrderPort})의 구현. 출고 안에서 지점 발주의 타입을 아는 유일한 곳이며,
 * 발주의 {@code port.in}만 호출하고 결과를 출고 자기 타입으로 변환한다. 발주를 OMS로 분리하면 이 클래스의 구현만 바뀐다.
 */
@Component
@RequiredArgsConstructor
public class StoreOrderFulfillmentAdapter implements StoreOrderPort {

    private final StoreOrderFulfillmentUseCase storeOrderFulfillmentUseCase;

    @Override
    public OrderView getOrder(Long storeOrderId) {
        return toView(storeOrderFulfillmentUseCase.getOrder(storeOrderId));
    }

    @Override
    public OrderView getOrderForUpdate(Long storeOrderId) {
        return toView(storeOrderFulfillmentUseCase.getOrderForUpdate(storeOrderId));
    }

    @Override
    public List<LineView> getLines(Long storeOrderId) {
        return storeOrderFulfillmentUseCase.getLines(storeOrderId).stream().map(this::toView).toList();
    }

    @Override
    public List<LineView> getLinesForUpdate(Long storeOrderId) {
        return storeOrderFulfillmentUseCase.getLinesForUpdate(storeOrderId).stream().map(this::toView).toList();
    }

    @Override
    public void increaseAllocated(List<LineQuantity> quantities) {
        storeOrderFulfillmentUseCase.increaseAllocated(toCommands(quantities));
    }

    @Override
    public void decreaseAllocated(List<LineQuantity> quantities) {
        storeOrderFulfillmentUseCase.decreaseAllocated(toCommands(quantities));
    }

    @Override
    public void applyPicked(List<LinePicked> picked) {
        storeOrderFulfillmentUseCase.applyPicked(picked.stream()
                .map(p -> new StoreOrderLinePickedCommand(p.storeOrderLineId(), p.allocatedQuantity(),
                        p.pickedQuantity()))
                .toList());
    }

    @Override
    public void refreshLineStatuses(Collection<Long> storeOrderLineIds) {
        storeOrderFulfillmentUseCase.refreshLineStatuses(storeOrderLineIds);
    }

    @Override
    public OrderView completeIfFulfilled(Long storeOrderId, Long changedBy) {
        return toView(storeOrderFulfillmentUseCase.completeIfFulfilled(storeOrderId, changedBy));
    }

    private List<StoreOrderLineQuantityCommand> toCommands(List<LineQuantity> quantities) {
        return quantities.stream()
                .map(q -> new StoreOrderLineQuantityCommand(q.storeOrderLineId(), q.quantity()))
                .toList();
    }

    private OrderView toView(StoreOrderFulfillmentOrderView order) {
        return new OrderView(order.storeOrderId(), order.orderNo(), order.warehouseId(),
                StoreOrderState.valueOf(order.status().name()));
    }

    private LineView toView(StoreOrderFulfillmentLineView line) {
        return new LineView(line.storeOrderLineId(), line.skuId(), line.unallocatedQuantity(),
                line.requestedUnitSupplyPrice());
    }
}
