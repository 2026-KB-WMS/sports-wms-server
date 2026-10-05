package com.kb.wms.outbound.application.port.in.result;

import java.util.List;

/**
 * 출고 생성 결과. 응답의 lineCount는 항목 수와 같다.
 */
public record OutboundCreateResult(
        OutboundView view,
        List<OutboundLineView> items
) {

    public int lineCount() {
        return items.size();
    }
}
