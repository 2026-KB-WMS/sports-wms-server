# N+1 조회 개선 기록

목록 API에서 항목 수만큼 쿼리가 나가는 N+1을 찾아 고친 이력과, 같은 방법으로 전후를 비교하는 절차를 적는다. 쿼리 횟수와 서버 처리시간은 `ApiTimingFilter`가 요청마다 남기는 로그(`GET /api/v1/products -> 200 (712ms, queries=43)`)를 쓴다.

- **개선율의 기준은 쿼리 횟수다.** 같은 요청이면 항상 같은 값이라 비교가 정확하다. 시간은 로컬 환경(JIT, 캐시, 다른 프로세스)에 따라 흔들려 참고용이다.
- 새 개선을 기록할 때는 이 문서 §3 표에 한 행을 추가하고, 원인과 해결은 §2 형식으로 적는다.

## 1. 측정 방법

`scripts/perf/measure_api.py`가 대상 API를 같은 조건으로 호출하고 서버 로그에서 쿼리 횟수와 처리시간을 읽는다.

```
# 수정 전 (develop 코드로 컨테이너가 떠 있는 상태)
python scripts/perf/measure_api.py --label before --out before.json

# 코드를 고치고 컨테이너를 다시 빌드한 뒤 (docker compose up -d --build)
python scripts/perf/measure_api.py --label after --out after.json --compare before.json
```

- 대상마다 워밍업 5회 후 `--repeat`(기본 30)회를 호출하고, 같은 구간의 로그에서 중앙값과 p95를 구한다.
- 측정 대상은 스크립트의 `TARGETS`에 있다. 다른 API를 재려면 `(이름, 호출 경로, 로그 경로 패턴)`을 추가한다.
- 전제: 컨테이너 이름이 `wms-app`이고 `ApiTimingFilter` 로그가 보여야 한다. 데이터는 `scripts/perf/seed_perf_small.py`로 만든 소규모 데이터(SKU 1,000개)다.
- 전후 비교는 **같은 데이터, 같은 환경, 같은 반복 횟수**여야 한다. 서버에 다른 요청이 섞이지 않게 쓰지 않을 때 실행한다.
- 쿼리 내용은 `WMS_APILOG_SHOWQUERIES`(docker-compose 기본 켜짐)로 콘솔에서 확인한다. 같은 모양의 쿼리가 `where x.id=?`로 반복되면 N+1이다.

## 2. 개선 내역

### 2-1. 상품 목록·SKU 목록 (#342)

**원인**

- `GET /products`: 컨트롤러가 목록의 상품마다 `brandQueryUseCase.getBrand(id)`와 `categoryUseCase.getCategory(id)`를 호출해 브랜드·카테고리 조회가 상품 수 × 2회 나갔다. 20건이면 43쿼리(사용자 1 + 목록 2 + 40), 100건이면 203쿼리였다.
- `GET /products/skus`: 옵션은 이미 `in` 조회 한 번이었지만, 상품명을 SKU마다 `productUseCase.getProduct(id)`로 읽었다. 상품당 한 번만 읽도록 캐시는 했어도 서로 다른 상품 수만큼 나갔다.

**해결**

- 이름을 ID 목록으로 한 번에 읽는 유스케이스를 추가했다: `BrandQueryUseCase.getBrandNames`, `CategoryUseCase.getCategoryNames`, `ProductUseCase.getProductNames`. 아웃바운드 포트에는 `findByIdIn(Collection<Long>)`을 추가했고 어댑터는 `findAllById`를 쓴다.
- 컨트롤러는 현재 페이지 항목의 ID를 모아 맵을 한 번 만들고 응답 변환에서 맵을 읽는다.
- 같은 도메인(product) 안의 조회라 ADR-007의 JPQL 조인 예외가 아니라 ID 일괄 조회로 풀었다.
- 응답 JSON은 바뀌지 않는다. 컨트롤러 테스트에 목록에서 단건 조회(`getBrand`, `getCategory`, `getProduct`)가 호출되지 않는지 검증을 넣어 되돌아오는 것을 막는다.

**결과** (로컬 docker-compose, SKU 1,000개, 워밍업 5회 후 30회)

| API | 쿼리 | 쿼리 변화 | 중앙값(ms) | 중앙값 변화 | p95(ms) | p95 변화 |
|---|---|---|---|---|---|---|
| 상품 목록 size=20 | 43 → 5 | -88% | 396.5 → 30.5 | -92% | 794 → 40 | -95% |
| 상품 목록 size=100 | 203 → 5 | -98% | 996.5 → 33.5 | -97% | 1806 → 43 | -98% |
| SKU 목록 size=20 | 14 → 7 | -50% | 86 → 33 | -62% | 119 → 40 | -66% |
| SKU 목록 size=100 | 39 → 7 | -82% | 184.5 → 39.5 | -79% | 289 → 49 | -83% |

- 수정 후 쿼리 수는 페이지 크기와 무관하게 상품 목록 5개, SKU 목록 7개로 일정하다.
- 남은 쿼리: 상품 목록은 사용자 조회, 목록, 건수, 브랜드, 카테고리다. SKU 목록은 여기에 옵션 값·옵션 그룹 조회가 더해진다.

### 2-2. 창고 구역 목록 (#344)

**원인**

- `GET /warehouses/sections`, `GET /warehouses/{warehouseId}/sections`: 컨트롤러가 목록의 구역마다 `warehouseUseCase.getWarehouse(id)`로 창고를, 상위 구역이 있으면 `warehouseSectionUseCase.getSection(parentId)`로 상위 구역 코드를 한 건씩 읽었다. 구역 28건에 52쿼리, 14건에 28쿼리였다.
- 같은 컨트롤러에 창고·상위 구역을 한 번만 읽도록 캐시하는 `toResponses`가 있었지만 어느 엔드포인트도 호출하지 않았다.

**해결**

- 창고 ID별 이름 `WarehouseUseCase.getWarehouseNames`, 구역 ID별 코드 `WarehouseSectionUseCase.getSectionCodes`를 추가했다. 아웃바운드 포트에는 `findByIdIn(Collection<Long>)`을 추가했다.
- 두 목록 엔드포인트가 `toResponses`를 쓰도록 바꾸고, `toResponses`는 현재 목록에 필요한 창고·상위 구역 ID만 모아 한 번에 읽도록 다시 썼다. 단건 조회·등록·수정 응답은 기존 `toResponse`를 그대로 쓴다.
- 응답 JSON은 바뀌지 않는다. 로컬 DB에서 28건 모두 창고명이 채워졌고, 상위 구역이 있는 22건 모두 상위 구역 코드가 채워졌다. 컨트롤러 테스트에 목록에서 `getWarehouse`, `getSection`이 호출되지 않는지 검증을 넣었다.

**결과** (로컬 docker-compose, 구역 28건, 창고 2개, 워밍업 5회 후 30회)

| API | 쿼리 | 쿼리 변화 | 중앙값(ms) | 중앙값 변화 | p95(ms) | p95 변화 |
|---|---|---|---|---|---|---|
| 창고 구역 목록(전체, 28건) | 52 → 4 | -92% | 78 → 17 | -78% | 118 → 21 | -82% |
| 창고 구역 목록(창고별, 14건) | 28 → 5 | -82% | 35 → 16.5 | -53% | 41 → 20 | -51% |

- 이 목록은 페이징이 없어 건수가 늘면 쿼리 수 차이가 더 벌어진다. 수정 후 쿼리 수는 구역 수와 무관하다.
- 같은 측정에서 코드를 바꾸지 않은 상품·SKU 목록도 시간이 +3%~+87% 흔들렸다(쿼리 수는 그대로). 컨테이너를 다시 띄운 직후 JIT가 덜 데워진 영향으로 보이며, 시간은 이처럼 참고용이고 개선율은 쿼리 횟수로 판단한다.

### 2-3. 지점·창고 담당자 목록 (#344)

**원인**

- `GET /stores/managers`: `StoreMemberController`가 담당자 행마다 `storeUseCase.getStore(member.storeId())`로 지점명을 읽었다. 20건에 23쿼리, 38건에 40쿼리였다.
- `GET /warehouses/managers`: `WarehouseMemberController`가 담당자 행마다 `warehouseUseCase.getWarehouse(member.warehouseId())`로 창고명을 읽었다. 20건에 23쿼리, 32건에 34쿼리였다.

**해결**

- 지점 ID별 이름 `StoreUseCase.getStoreNames`를 추가했다(`StoreRepository.findByIdIn`). 창고 쪽은 2-2에서 추가한 `WarehouseUseCase.getWarehouseNames`를 그대로 쓴다.
- 두 컨트롤러는 현재 페이지 담당자의 지점·창고 ID를 모아 이름 맵을 한 번 만들고 응답 변환에서 맵을 읽는다. 담당자 등록(`POST`) 응답은 한 건이라 기존 `toResponse`를 그대로 쓴다.
- 응답 JSON은 바뀌지 않는다. 로컬 DB에서 지점 담당자 38건, 창고 담당자 32건 모두 이름이 채워졌다. 컨트롤러 테스트에 목록에서 `getStore`, `getWarehouse`가 호출되지 않는지 검증을 넣었다.

**결과** (로컬 docker-compose, 워밍업 5회 후 30회)

| API | 쿼리 | 쿼리 변화 | 중앙값(ms) | 중앙값 변화 | p95(ms) | p95 변화 |
|---|---|---|---|---|---|---|
| 지점 담당자 목록 size=20 | 23 → 4 | -83% | 37 → 13 | -65% | 48 → 16 | -67% |
| 지점 담당자 목록 size=100 (38건) | 40 → 3 | -92% | 62 → 12 | -81% | 79 → 16 | -80% |
| 창고 담당자 목록 size=20 | 23 → 4 | -83% | 29 → 11 | -62% | 40 → 13 | -68% |
| 창고 담당자 목록 size=100 (32건) | 34 → 3 | -91% | 47 → 9 | -81% | 89 → 12 | -87% |

- 수정 후 쿼리 수는 건수와 무관하게 3~4개(사용자 조회, 목록, 건수, 이름)다. `size=100`에서는 한 페이지에 다 담겨 건수 쿼리가 생략된다.
- 이 측정에서도 코드를 바꾸지 않은 상품·SKU·구역 목록의 시간이 컨테이너 재시작 직후라 +27%~+120% 흔들렸다. 개선율은 쿼리 횟수로 판단한다.

### 2-4. 옵션 그룹 조회 3종 (#346)

**원인**

- `GET /products/{productId}/option-groups`: `OptionGroupService`가 SKU마다 `findBySkuId`, 옵션 값마다 `findById`, 그룹마다 `findById`를 호출했다. SKU 수와 옵션 값 수에 비례해 쿼리가 늘어난다.
- `GET /products/option-groups`: 옵션 그룹마다 `findByOptionGroupId`로 옵션 값을 읽었다.
- `GET /products/categories/{categoryId}/option-groups`: `CategoryOptionGroupService`가 지정된 그룹마다 `findByOptionGroupId`로 옵션 값을 읽었다.

**해결**

- `OptionValueRepository.findByOptionGroupIdIn`을 추가했다. 이미 있던 `SkuOptionValueRepository.findBySkuIdIn`, `OptionValueRepository.findAllByIds`, `OptionGroupRepository.findAllByIds`와 함께 쓴다.
- 세 조회 모두 필요한 ID를 모아 한 번씩 읽고 메모리에서 그룹별로 나눈다. 정렬 기준(그룹 ID 오름차순, 옵션 값 `sortOrder` 오름차순)과 응답 JSON은 그대로다.
- 서비스 테스트에 단건 조회(`findBySkuId`, `findById`, `findByOptionGroupId`)가 호출되지 않는지 검증을 넣었다.

**결과** (로컬 docker-compose, 워밍업 5회 후 30회)

| API | 쿼리 | 쿼리 변화 | 중앙값(ms) | 중앙값 변화 | p95(ms) | p95 변화 |
|---|---|---|---|---|---|---|
| 옵션 그룹 전체 (10건) | 12 → 3 | -75% | 19 → 11 | -42% | 31 → 17 | -45% |
| 상품별 옵션 그룹 (SKU 4개) | 12 → 6 | -50% | 15 → 14 | -7% | 22 → 25 | +14% |
| 카테고리별 옵션 그룹 | 9 → 5 | -44% | 15.5 → 13.5 | -13% | 21 → 17 | -19% |

- 상품별 조회는 시드 데이터에서 SKU가 가장 많은 상품도 4개뿐이라 개선 폭이 작게 보인다. 수정 후 쿼리 수(6)는 SKU 수와 무관하고, 수정 전에는 SKU와 옵션 값이 늘수록 계속 늘어나는 구조였다(코드 기준 추정이며 SKU가 많은 상품으로 실측하지는 않았다).
- 상품별 조회의 시간은 변화가 측정 오차 안이다. 쿼리 수로 판단한다.

## 3. 요약 표

| 날짜 | 이슈 | 대상 API | 쿼리(수정 전 → 후) | 중앙값(ms) | 비고 |
|---|---|---|---|---|---|
| 2026-11-11 | #342 | `GET /products` (size=20) | 43 → 5 | 396.5 → 30.5 | 브랜드·카테고리 일괄 조회 |
| 2026-11-11 | #342 | `GET /products` (size=100) | 203 → 5 | 996.5 → 33.5 | 위와 같음 |
| 2026-11-11 | #342 | `GET /products/skus` (size=20) | 14 → 7 | 86 → 33 | 상품명 일괄 조회 |
| 2026-11-11 | #342 | `GET /products/skus` (size=100) | 39 → 7 | 184.5 → 39.5 | 위와 같음 |
| 2026-11-11 | #344 | `GET /warehouses/sections` (28건) | 52 → 4 | 78 → 17 | 창고명·상위 구역 코드 일괄 조회 |
| 2026-11-11 | #344 | `GET /warehouses/{id}/sections` (14건) | 28 → 5 | 35 → 16.5 | 위와 같음 |
| 2026-11-11 | #344 | `GET /stores/managers` (size=20) | 23 → 4 | 37 → 13 | 지점명 일괄 조회 |
| 2026-11-11 | #344 | `GET /stores/managers` (size=100, 38건) | 40 → 3 | 62 → 12 | 위와 같음 |
| 2026-11-11 | #344 | `GET /warehouses/managers` (size=20) | 23 → 4 | 29 → 11 | 창고명 일괄 조회 |
| 2026-11-11 | #344 | `GET /warehouses/managers` (size=100, 32건) | 34 → 3 | 47 → 9 | 위와 같음 |
| 2026-11-12 | #346 | `GET /products/option-groups` (10건) | 12 → 3 | 19 → 11 | 옵션 값 일괄 조회 |
| 2026-11-12 | #346 | `GET /products/{id}/option-groups` (SKU 4개) | 12 → 6 | 15 → 14 | SKU 수와 무관한 쿼리 수 |
| 2026-11-12 | #346 | `GET /products/categories/{id}/option-groups` | 9 → 5 | 15.5 → 13.5 | 옵션 값 일괄 조회 |

## 4. 남은 후보

2026-11-11에 모든 GET 목록·상세 API를 소규모 데이터(SKU 1,000개)로 한 번씩 호출해 요청당 쿼리 수를 확인했다(`size=20`, `size=100`). 대부분의 목록은 건수와 무관하게 2~7쿼리였다. 건수에 비례해 늘어난 곳은 구역 목록(§2-2)과 담당자 목록(§2-3)이고 이미 고쳤다. 상세 API는 2026-11-12에 라인 수가 다른 건끼리 비교해 건수에 비례하는지 확인했다(§4-1). 목록·상세 GET API에서 확인된 N+1은 모두 고쳤다.

### 4-1. 상세 API 쿼리 수 확인

같은 상세 API를 응답 안 배열(라인) 크기가 다른 건에 호출해 쿼리 수를 비교했다. 처음에 한 번씩 호출해 크기를 모으고, 크기별로 대표 건을 골라 다시 재서 로그의 쿼리 수를 읽었다.

| API | 라인 수 범위 | 쿼리 |
|---|---|---|
| `GET /inbounds/{id}/details` | 10~100 | 3 |
| `GET /purchase-orders/{id}/details` | 11~100 | 4 |
| `GET /orders/{id}/details` | 1~5 | 6 |
| `GET /outbounds/{id}/details` | 1~7 | 3 |
| `GET /supplier-orders/{id}` | 10~100 | 3 |
| `GET /lots/{id}` | 1~2 | 6 |
| `GET /products/{id}` | 0~2 | 5(스펙 0개) / 7(1개 이상) |
| `GET /products/skus/{id}` | 1~3 | 8 |

- 라인 수가 달라도 쿼리 수는 고정이다. 6~8쿼리는 건수와 무관한 상세 조립 비용이다.
- 상품 상세만 스펙이 0개에서 1개 이상이 될 때 5 → 7로 늘고 이후는 같다.
- 한계: 로트·상품·SKU 상세는 시드 데이터의 배열이 0~3개뿐이라 많은 항목에서는 보지 못했다. 배당 상세(`GET /allocations/{id}`)는 배열 크기가 한 종류뿐이라 비교하지 못했다.

### 4-2. 확인하지 못한 범위

- POST·PUT 등 쓰기 API는 보지 않았다.
- 소규모 시드 데이터(SKU 1,000개, 거래 10,000건) 기준이다. 더 큰 데이터에서 드러나는 것은 성능 시나리오에서 본다.

### 4-3. N+1은 아니지만 함께 볼 후보

| 대상 | 내용 | 확인 근거 |
|---|---|---|
| `GET /dashboard/hq/summary` | 요청 한 번에 26쿼리. N+1은 아니고 집계 쿼리 묶음이다. 안전 재고 기준 SKU와 창고·SKU별 가용 재고를 전부 읽는 쿼리는 SKU 수에 비례해 커진다. | 성능 시나리오 S9에서 측정 |
| 모든 인증 요청의 사용자 조회 1회 | 요청마다 사용자를 다시 읽는 설계다([ADR-016](../adr/016-reload-user-per-request.md)). N+1이 아니라 의도된 비용이고, 쿼리 횟수에 항상 1건 포함된다. | ADR-016 |

## 5. 기록 양식

새 개선은 §2에 아래 형식으로 추가한다.

```
### 2-N. <대상> (#이슈)
**원인** — 어떤 코드가 몇 번 쿼리를 내는지
**해결** — 무엇을 어떻게 바꿨는지, 응답이 그대로인지
**결과** — measure_api.py 비교 표 (환경·데이터 규모·반복 횟수 포함)
```
