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

## 3. 요약 표

| 날짜 | 이슈 | 대상 API | 쿼리(수정 전 → 후) | 중앙값(ms) | 비고 |
|---|---|---|---|---|---|
| 2026-11-11 | #342 | `GET /products` (size=20) | 43 → 5 | 396.5 → 30.5 | 브랜드·카테고리 일괄 조회 |
| 2026-11-11 | #342 | `GET /products` (size=100) | 203 → 5 | 996.5 → 33.5 | 위와 같음 |
| 2026-11-11 | #342 | `GET /products/skus` (size=20) | 14 → 7 | 86 → 33 | 상품명 일괄 조회 |
| 2026-11-11 | #342 | `GET /products/skus` (size=100) | 39 → 7 | 184.5 → 39.5 | 위와 같음 |

## 4. 남은 후보

코드 가이드와 로그로 확인된 것만 적는다. 전 API를 훑은 목록이 아니다.

| 대상 | 내용 | 확인 근거 |
|---|---|---|
| 창고 구역 목록(`WarehouseSectionController`의 `getAllSections`, `getSectionsByWarehouse`) | 목록의 구역마다 창고와 상위 구역을 조회한다. `toResponses`가 정의돼 있지만 호출되지 않는다. | [`code-guide/warehouse/adapter/in/web/WarehouseSectionController.md`](../code-guide/warehouse/adapter/in/web/WarehouseSectionController.md) |
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
