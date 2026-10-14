# ProductSkuService

`product/application/service/ProductSkuService.java` · 구현: `ProductSkuUseCase` · 클래스 기본 `@Transactional(readOnly = true)`

SKU 등록·수정·조회·상태 변경과 옵션 연결.

## 점주 가시성 규칙

- `actor`를 받는 메서드(사용자 요청용): 점주(STORE_OWNER)에게는 **활성 SKU만** 보인다. 목록은 활성으로 고정, 단건은 비활성이면 404.
- `actor`가 없는 메서드(다른 도메인의 내부 조회)는 항상 전체를 반환한다.
- 점주 응답에서 매입 단가·안전 재고를 숨기는 건 웹 어댑터가 `ProductSku.isPurchaseInfoVisibleTo`로 판단해 DTO 변환 시 처리한다.

## 상수

`SKU_CODE_MAX_LENGTH = 50`, `SKU_NAME_MAX_LENGTH = 200`

## 메서드

| 메서드 | 트랜잭션 | 설명 |
|---|---|---|
| `registerSku(command)` | 쓰기 | 상품 존재·활성 확인(`PRODUCT_NOT_FOUND`, `PRODUCT_INACTIVE`) → 단위가 있으면 `SkuUnit` 허용 검사 → SKU 옵션 검증 → 옵션 조합 중복 검사 → SKU 코드·이름 자동 생성 → 코드·바코드 중복 검사 → 저장 → 옵션 연결 저장 |
| `updateSku(command, actor)` | 쓰기 | 변경 필드 없음 → `VALIDATION_ERROR`. 없음 → `SKU_NOT_FOUND`. 다른 SKU와 겹치는 바코드 → `DUPLICATE_BARCODE`(자기 바코드 재전송은 허용). 단가(매입·공급)가 하나라도 바뀌면 `SkuPriceHistory`를 `actor.userId()`로 기록 |
| `getSkus(condition)` | 읽기 | `productId`·`brandId`·`categoryId` 조건이 있으면 각각 존재 확인 후 검색 |
| `getSkus(condition, actor)` | 읽기 | 점주면 `isActive=true`로 고정 |
| `getSku(skuId)` | 읽기 | 단건 조회. 없으면 `SKU_NOT_FOUND` |
| `getSku(skuId, actor)` | 읽기 | 점주에게 비활성 SKU는 `SKU_NOT_FOUND` |
| `changeSkuStatus(skuId, active)` | 쓰기 | 활성화: 상품이 비활성이면 `PRODUCT_INACTIVE`. 비활성화: 활성 SKU면 `isSkuInUse` 검사 → `SKU_IN_USE`. 이미 비활성인 SKU는 검사 없이 통과 |
| `connectOptions(command)` | 쓰기 | 옵션이 **없는** 기존 SKU에 한 번만 연결. 이미 있으면 `SKU_OPTIONS_ALREADY_CONNECTED`. 등록 때와 같은 검증(카테고리 지정·필수 그룹·조합 중복) |
| `getSkuOptions(skuId)` | 읽기 | 한 SKU의 옵션 요약 목록 |
| `getSkuOptionsBySkuIds(skuIds)` | 읽기 | 여러 SKU의 옵션 요약을 `skuId → 목록` 맵으로 반환(그룹명 포함) |

## 자동 생성 규칙

| 항목 | 규칙 | 실패 시 |
|---|---|---|
| SKU 코드 | `상품코드-옵션코드-옵션코드...` (카테고리 지정 순서). 옵션이 없으면 상품 코드 그대로 | 옵션 값에 코드가 없으면 `VALIDATION_ERROR`, 50자 초과면 `VALIDATION_ERROR` |
| SKU 이름 | `상품명 (옵션값/옵션값)`. 옵션이 없으면 상품명 그대로 | 200자 초과면 `VALIDATION_ERROR` |

## 옵션 조합 중복 검사 (`assertCombinationAvailable`)

같은 상품의 다른 SKU(**비활성 포함**)가 정확히 같은 옵션 값 집합을 가지면 `DUPLICATE_SKU_OPTION_COMBINATION`. 옵션 연결 시에는 자기 자신을 제외한다.
