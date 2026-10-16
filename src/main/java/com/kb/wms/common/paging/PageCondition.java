package com.kb.wms.common.paging;

import java.util.List;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;

/**
 * 목록 조회의 페이지·정렬 조건. 포트(UseCase·Repository)가 검색 조건과 함께 주고받는다.
 *
 * @param page   1부터 시작하는 페이지 번호
 * @param size   페이지 크기
 * @param orders 적용할 정렬. 마지막은 항상 ID 보조 정렬이다
 */
public record PageCondition(int page, int size, List<SortOrder> orders) {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageCondition {
        orders = List.copyOf(orders);
    }

    /**
     * 요청 파라미터를 읽어 검증한다. 생략하면 {@code page}=1, {@code size}=20, {@code sort}=기본 정렬이다.
     * {@code sort}는 {@code 필드,asc|desc} 형식이고 방향을 생략하면 asc다. 정렬은 하나만 받는다.
     *
     * @throws BusinessException 범위를 벗어난 page·size, 허용되지 않은 sort는 400 VALIDATION_ERROR
     */
    public static PageCondition of(Integer page, Integer size, String sort, SortSpec spec) {
        int resolvedPage = page == null ? DEFAULT_PAGE : page;
        int resolvedSize = size == null ? DEFAULT_SIZE : size;
        if (resolvedPage < 1) {
            throw validation("page는 1 이상이어야 합니다.");
        }
        if (resolvedSize < 1 || resolvedSize > MAX_SIZE) {
            throw validation("size는 1 이상 " + MAX_SIZE + " 이하여야 합니다.");
        }
        return new PageCondition(resolvedPage, resolvedSize, spec.withTieBreaker(resolveOrders(sort, spec)));
    }

    /** 데이터베이스에서 건너뛸 행 수 */
    public long offset() {
        return (long) (page - 1) * size;
    }

    /** 응답 {@code pageInfo.sort}에 내려줄 값. 사용자에게 보이는 첫 정렬 기준이다 (예: {@code createdAt,desc}). */
    public String sortLabel() {
        SortOrder first = orders.get(0);
        return first.field() + "," + first.direction().lowerName();
    }

    private static List<SortOrder> resolveOrders(String sort, SortSpec spec) {
        if (sort == null || sort.isBlank()) {
            return spec.defaultOrders();
        }
        String[] parts = sort.split(",", -1);
        if (parts.length > 2) {
            throw validation("sort는 '필드,asc|desc' 형식이며 정렬 기준은 하나만 지정할 수 있습니다.");
        }
        String field = parts[0].trim();
        String property = spec.fields().get(field);
        if (property == null) {
            throw validation("sort 필드는 " + String.join(", ", spec.fields().keySet()) + " 중 하나여야 합니다.");
        }
        SortDirection direction = SortDirection.ASC;
        if (parts.length == 2) {
            direction = SortDirection.parse(parts[1])
                    .orElseThrow(() -> validation("sort 방향은 asc 또는 desc여야 합니다."));
        }
        return List.of(new SortOrder(field, property, direction));
    }

    private static BusinessException validation(String message) {
        return new BusinessException(ErrorCode.VALIDATION_ERROR, message);
    }
}
