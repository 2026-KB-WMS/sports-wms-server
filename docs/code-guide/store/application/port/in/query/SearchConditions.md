# 검색 조건 (SearchCondition)

`store/application/port/in/query/`

## StoreSearchCondition

| 항목 | 내용 |
|---|---|
| 용도 | 지점 목록을 필터링한다. **null인 조건은 무시** |
| 엔드포인트 | GET `/api/v1/stores` (본사 전용) |
| 만드는 곳 | `StoreController.getStores` |
| 쓰는 곳 | `StoreService.getStores` → `StoreRepository.search` |
| 정렬 | 생성 일시 내림차순, 같으면 `storeId` 내림차순 |

| 필드 | 설명 |
|---|---|
| keyword | 지점명·지점 코드 부분 일치(대소문자 무시) |
| isActive | 운영 상태 필터 (null이면 전체) |
