# 페이지네이션·정렬 (paging)

`common/paging/` · 목록 API의 `page`·`size`·`sort`를 읽고 검증하는 공통 코드. Spring에 의존하지 않아 포트(UseCase·Repository)가 그대로 주고받는다. 결정 배경은 [ADR-032](../../../adr/032-pagination-and-sorting.md).

## 클래스

| 클래스 | 역할 |
|---|---|
| `SortSpec` | API 하나의 정렬 규칙. 정렬 가능한 필드(API 이름 → 쿼리 경로), 기본 정렬, ID 보조 정렬. 컨트롤러 상수로 정의 |
| `PageCondition` | `page`(1부터), `size`, 적용할 정렬 목록. `of(page, size, sort, spec)`이 검증하고 만든다 |
| `PageResult<T>` | 한 페이지 결과: `items`, `page`, `size`, `totalElements`, `sort`. `totalPages()`, `map()` |
| `SortOrder`, `SortDirection` | 정렬 기준 하나(API 필드명, 쿼리 경로, asc/desc) |

## 규칙

- `page` 기본 1, `size` 기본 20(1~100), `sort`는 `필드,asc|desc` 하나(방향 생략 시 asc). 어긋나면 `BusinessException(VALIDATION_ERROR)` → 400.
- 어떤 정렬이든 마지막에 `tieBreaker`(ID)를 첫 정렬과 같은 방향으로 붙인다. 이미 그 경로로 정렬하면 붙이지 않는다.
- `PageCondition.sortLabel()`은 응답 `pageInfo.sort`에 나가는 첫 정렬 기준이다.

## 도메인에서 쓰는 순서

```java
// 컨트롤러: 정렬 규칙은 API마다 상수로
private static final SortSpec SORT = SortSpec.builder()
        .field("createdAt", "i.createdAt")
        .field("status", "i.status")
        .defaultOrder("createdAt", SortDirection.DESC)   // 기존 고정 정렬 = 기본 정렬
        .tieBreaker("i.inboundId")
        .build();

PageCondition page = PageCondition.of(pageParam, sizeParam, sortParam, SORT);
return ApiResponse.ok(PageResponse.of(useCase.search(condition, page), InboundSummaryResponse::from));

// 영속성 어댑터: Pageable로 바꿔 Page<T>를 받고 PageResult로 되돌린다
Page<InboundSummary> result = jpaRepository.search(..., PageAdapter.toPageable(page));
return PageAdapter.toPageResult(result, page);
```

- 쿼리 경로는 JPQL 별칭 경로이거나 집계식(`count(l)`, `sum(...)`)이어야 하고, 생성자 프로젝션이 `group by`·집계를 쓰면 `countQuery`를 직접 적는다.
- 페이지네이션을 적용하는 목록과 정렬 필드는 도메인 서브이슈에서 정한다. 적용하지 않는 목록은 `ItemsResponse`를 그대로 쓴다.
