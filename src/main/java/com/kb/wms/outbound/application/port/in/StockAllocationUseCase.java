package com.kb.wms.outbound.application.port.in;

import java.util.List;

import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.common.paging.PageResult;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.outbound.application.port.in.command.StockAllocateCommand;
import com.kb.wms.outbound.application.port.in.command.StockAllocationReleaseCommand;
import com.kb.wms.outbound.application.port.in.query.StockAllocationSearchCondition;
import com.kb.wms.outbound.application.port.in.result.StockAllocateResult;
import com.kb.wms.outbound.application.port.in.result.StockAllocationDetail;
import com.kb.wms.outbound.application.port.in.result.StockAllocationReleaseResult;
import com.kb.wms.outbound.application.port.in.result.StockAllocationSummary;

/**
 * 재고 할당 유스케이스: 발주 FEFO 할당, 할당 해제, 조회.
 */
public interface StockAllocationUseCase {

    /**
     * 배정된(ASSIGNED) 발주의 잔여 수량을 FEFO 순서로 재고 행에 예약한다. 가용 재고가 부족한 항목은 가용 수량만큼만
     * 할당하고 부족분을 결과의 shortages로 돌려준다. 하나도 할당하지 못하면 409 INSUFFICIENT_STOCK으로 거절한다.
     */
    StockAllocateResult allocate(StockAllocateCommand command, AuthenticatedUser actor);

    /** ALLOCATED 할당을 해제한다. 취소되지 않은 출고에 연결된 할당은 해제할 수 없다. */
    StockAllocationReleaseResult release(StockAllocationReleaseCommand command, AuthenticatedUser actor);

    PageResult<StockAllocationSummary> searchAllocations(StockAllocationSearchCondition condition, PageCondition page,
                                                         AuthenticatedUser actor);

    StockAllocationDetail getAllocation(Long allocationId, AuthenticatedUser actor);
}