# Product API 명세

> 기준은 레포. 2026-10-02 Notion(최종 수정 2026-09-24)에서 이전. 공통 규칙은 [conventions.md](conventions.md) 참고.
> Base path: `/api/v1/products`. 목록 API는 `data.items`만 반환(페이지네이션 보류, ADR/conventions 메모 참고). 404는 도메인별 코드 사용.

## 구현 대비 메모

- 코드에만 있고 Notion 명세가 없던 엔드포인트: `PATCH /api/v1/products/skus/{skuId}/status` (SKU 활성/비활성). 아래 "PATCH /products/skus/{skuId}/status" 절에 코드 기준으로 명세를 보강했다.
- 상품 비활성화(`PATCH /products/{productId}`의 `isActive=false`)는 같은 트랜잭션에서 하위 ACTIVE SKU를 모두 비활성화한다(구현됨). 상품을 다시 활성화해도 SKU는 자동으로 활성화되지 않는다(SKU 상태 변경 API로 개별 복구).
- 상품 비활성화 차단(#194): 상품의 SKU에 재고(보유·할당 수량 > 0)가 있거나 진행 중인 입고(`ARRIVED`·`INSPECTING`)·창고 발주(`REQUESTED`·`CONFIRMED`)·출고(`READY`·`PICKING`·`PICKED`·`SHIPPED`)·지점 발주(`REQUESTED`·`APPROVED`·`ASSIGNED`·`ON_HOLD`) 항목이 있으면 409 `PRODUCT_IN_USE`다(창고 비활성화 `WAREHOUSE_IN_USE`와 같은 상태 기준). 출고는 SKU를 직접 갖지 않아 출고가 속한 지점 발주의 항목으로 판단한다. SKU 단건 비활성화(`PATCH /products/skus/{skuId}/status`)에도 같은 검사를 SKU 단위로 적용한다(409 `SKU_IN_USE`, [ADR-014](../adr/014-reactivation-and-sku-deactivation-guard.md)). 상품 비활성화가 하위 SKU를 함께 비활성화할 때는 상품 단위 검사가 이미 끝났으므로 SKU 검사를 다시 하지 않는다. 검사와 비활성화 사이에 새 재고·발주가 생기는 경합은 막지 않는다(창고 구역처럼 행을 잠그지 않음).
- 목록 API(상품·브랜드·카테고리·SKU)는 `page`·`size`·`sort`를 받지 않고 고정 정렬을 쓴다(각 절의 "정렬" 참고).
- Notion 명세의 `pageInfo`, 일반 `NOT_FOUND`는 현재 구현 기준(페이지네이션 보류, 도메인별 404)과 다름 → conventions.md 기준 따름.
- 점주(STORE_OWNER) 제한(#182): 상품·SKU 목록은 `isActive` 필터와 상관없이 활성 항목만, 비활성 상품·SKU 단건은 404(`PRODUCT_NOT_FOUND`/`SKU_NOT_FOUND`)다. SKU 목록·단건 응답에서 `currentPurchasePrice`·`safetyStockQuantity` 필드는 점주에게 생략한다. 값이 없는(미설정) 경우도 필드를 생략하므로, 본사·창고 관리자 응답에서도 `null` 대신 필드가 빠진다. SKU의 점주 제한(활성만)은 명세에 명시돼 있지 않아 상품과 같게 적용했다.
- SKU 단가 필수·수정(#207, [ADR-017](../adr/017-sku-price-required-and-update.md)): SKU 등록의 매입·공급 단가는 필수이고(0 이상, 0 허용) DB도 `NOT NULL`이다. 등록 후 `PATCH /products/skus/{skuId}`로 이름·바코드·중량·단가·안전재고를 수정하며, 단가가 바뀌면 처리자와 함께 이력(`sku_price_history`)을 남긴다.
- 미결: 카테고리 최대 depth, SKU 옵션 조합 중복 허용 규칙.

## 엔드포인트 목록 (15 + 코드 전용 1)

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| POST | /products | HQ_ADMIN | 상품 등록 |
| GET | /products | 전체 | 상품 목록 |
| GET | /products/{productId} | 전체 | 상품 상세 |
| PATCH | /products/{productId} | HQ_ADMIN | 상품 수정 |
| GET | /products/brands | 전체 | 브랜드 목록 |
| POST | /products/brands | HQ_ADMIN | 브랜드 등록 |
| PATCH | /products/brands/{brandId} | HQ_ADMIN | 브랜드 수정(이름·설명) |
| GET | /products/categories | 전체 | 카테고리 목록 |
| POST | /products/categories | HQ_ADMIN | 카테고리 등록 |
| POST | /products/option-groups | HQ_ADMIN | 옵션 그룹 등록 |
| POST | /products/option-groups/{optionGroupId}/values | HQ_ADMIN | 옵션 값 등록 |
| GET | /products/{productId}/option-groups | 전체 | 상품의 옵션 그룹 조회 |
| GET | /products/skus | 전체 | SKU 목록 |
| POST | /products/skus | HQ_ADMIN | SKU 등록 |
| GET | /products/skus/{skuId} | 전체 | SKU 상세 |
| POST | /products/skus/{skuId}/options | HQ_ADMIN | SKU 옵션 연결 |
| PATCH | /products/skus/{skuId}/status | HQ_ADMIN | SKU 활성/비활성 변경 (코드 전용, Notion 명세 없음) |

공통 에러: 400 검증 실패, 401 미인증, 403 권한 없음.

## POST /products — 상품 등록 (P0)

- 권한: HQ_ADMIN
- Body: `brandId`, `categoryId`, `productCode`(≤50, unique), `productName`(≤200), `description`(선택)
- 응답: 201, 생성된 상품(status ACTIVE)
- 에러: `BRAND_NOT_FOUND` 404, `CATEGORY_NOT_FOUND` 404, `BRAND_INACTIVE` 409, `CATEGORY_INACTIVE` 409, `DUPLICATE_PRODUCT_CODE` 409

## GET /products — 상품 목록

- 권한: 전체. STORE_OWNER는 활성 상품만 조회.
- Query: `categoryId`, `brandId`, `keyword`, `isActive`. `page`·`size`·`sort`는 받지 않는다(페이지네이션 보류).
- 정렬은 고정(등록 일시 `createdAt` 내림차순, 같으면 `productId` 내림차순).
- 응답: `data.items[]`
- 에러: 400, 401, 404 `BRAND_NOT_FOUND`(존재하지 않는 `brandId` 필터), 404 `CATEGORY_NOT_FOUND`(존재하지 않는 `categoryId` 필터)

## GET /products/{productId} — 상품 상세

- 에러: `PRODUCT_NOT_FOUND` 404. STORE_OWNER가 비활성 상품 조회 시에도 404.

## PATCH /products/{productId} — 상품 수정

- 권한: HQ_ADMIN
- Body(부분 수정, 최소 1개 필드): `productName`, `description`, `brandId`, `categoryId`, `isActive`
- `productCode`는 수정 불가(포함 시 400).
- 변경 대상 brand/category는 활성이어야 함, 아니면 409(`BRAND_INACTIVE`/`CATEGORY_INACTIVE`).
- 멱등. 상품 비활성화(`isActive=false`) 시 하위 ACTIVE SKU를 같은 트랜잭션에서 함께 비활성화한다(재고 도메인은 SKU 상태만 보기 때문). 이미 비활성인 SKU는 그대로 두며, 상품을 다시 활성화(`isActive=true`)해도 SKU는 자동으로 활성화되지 않는다.
- 비활성화(`isActive=false`)는 재고가 남아 있거나 진행 중인 입고·창고 발주·출고·지점 발주가 있으면 409 `PRODUCT_IN_USE`이고 상품·SKU 상태는 바뀌지 않는다. 이미 비활성인 상품에 다시 `false`를 보내는 요청은 검사 없이 기존처럼 성공한다.
- 에러: `PRODUCT_NOT_FOUND`, `BRAND_NOT_FOUND`, `CATEGORY_NOT_FOUND` 404, `BRAND_INACTIVE`·`CATEGORY_INACTIVE`·`PRODUCT_IN_USE` 409

## GET /products/brands, POST /products/brands

- POST(HQ_ADMIN): `brandName`(≤100, unique), `description`(≤500). 에러 `DUPLICATE_BRAND_NAME` 409. 201.
- GET: query `keyword`, `isActive`. 정렬은 고정(`brandName` 오름차순, 같으면 `brandId` 오름차순). 응답 `data.items[]`.

## PATCH /products/brands/{brandId} — 브랜드 수정 (#216)

- 권한: HQ_ADMIN
- Body(부분 수정, 최소 1개 필드): `brandName`(≤100, 공백 불가), `description`(≤500). 값이 있는 필드만 바꾼다.
- 응답: `brandId, brandName, description, isActive`(목록 항목과 같음).
- 에러: 400(필드 없음, 이름 공백·초과, 설명 초과), 403, 404 `BRAND_NOT_FOUND`, 409 `DUPLICATE_BRAND_NAME`(다른 브랜드가 쓰는 이름. 자기 이름 그대로는 통과)
- 비활성 브랜드도 수정할 수 있고 활성 상태는 바뀌지 않는다. 이미 연결된 상품은 영향이 없다.
- 브랜드·카테고리의 **비활성화·재활성화 API는 두지 않는다**(2026-10-10 결정, 필요한 업무가 없음).

## GET /products/categories, POST /products/categories

- POST(HQ_ADMIN): `parentCategoryId`(선택), `categoryCode`(≤50, unique), `categoryName`(≤100), `sortOrder`(기본 0). `depth`는 서버가 계산.
- 201. 응답(POST·GET 공통 항목): `categoryId, parentCategoryId, categoryCode, categoryName, depth, sortOrder, isActive, createdAt`
- 에러: `PARENT_CATEGORY_NOT_FOUND` 404, `DUPLICATE_CATEGORY_CODE` 409, `PARENT_CATEGORY_INACTIVE` 409
- 미결: 카테고리 최대 depth 제한.
- GET: query `parentCategoryId`, `depth`, `keyword`, `isActive`. 정렬은 고정(`sortOrder` 오름차순, 같으면 `categoryId` 오름차순). 에러: 404 `CATEGORY_NOT_FOUND`(존재하지 않는 `parentCategoryId` 필터).

## POST /products/option-groups

- Body: `name`(≤100, unique). 에러 `DUPLICATE_OPTION_GROUP_NAME` 409. 상태 컬럼 없음.

## POST /products/option-groups/{optionGroupId}/values

- Body: `value`(≤100, 그룹 내 unique), `sortOrder`
- 에러: `OPTION_GROUP_NOT_FOUND` 404, `DUPLICATE_OPTION_VALUE` 409

## GET /products/{productId}/option-groups

- 페이지네이션 없이 전체 반환. 에러 `PRODUCT_NOT_FOUND` 404.

## POST /products/skus — SKU 등록

- 권한: HQ_ADMIN
- Body: `productId`, `skuCode`(≤50), `barcode`(선택, ≤100, unique), `skuName`(≤200), `weight`(선택), `currentPurchasePrice`(**필수**, 0 이상), `currentSupplyPrice`(**필수**, 0 이상), `unit`(기본 `EA`), `safetyStockQuantity`(기본 0, BIGINT)
- 에러: 400 `VALIDATION_ERROR`(단가 누락·음수 포함), `PRODUCT_NOT_FOUND` 404, `PRODUCT_INACTIVE` 409, `DUPLICATE_SKU_CODE` 409, `DUPLICATE_BARCODE` 409
- 미결: 동일 옵션 조합 SKU 중복 허용 규칙.

## PATCH /products/skus/{skuId} — SKU 수정 (코드 추가, #207)

- 권한: HQ_ADMIN(마스터 데이터, [ADR-003](../adr/003-master-data-owned-by-hq.md)). 비활성 SKU도 수정할 수 있다.
- Body: 모든 필드가 선택이며 값이 있는 필드만 수정한다. **최소 1개 필요**(없으면 400).

| 필드 | 규칙 |
|---|---|
| skuName | 1~200자 |
| barcode | ≤100, 다른 SKU와 겹치면 409 `DUPLICATE_BARCODE`(자기 바코드를 다시 보내는 것은 허용). 비우기는 지원하지 않음 |
| weight | 0 이상 |
| currentPurchasePrice | 0 이상 |
| currentSupplyPrice | 0 이상 |
| safetyStockQuantity | 0 이상, BIGINT |

- `skuCode`, `productId`, `unit`, `isActive`를 보내면 400 `VALIDATION_ERROR`다(상태는 `PATCH /products/skus/{skuId}/status`).
- 200. 응답: `skuId, productId, skuCode, barcode, skuName, weight, currentPurchasePrice, currentSupplyPrice, unit, safetyStockQuantity, isActive, updatedAt`
- 에러: 400 `VALIDATION_ERROR`, 401, 403(HQ_ADMIN이 아님), 404 `SKU_NOT_FOUND`, 409 `DUPLICATE_BARCODE`
- 단가 이력: 매입·공급 단가 중 하나라도 실제로 바뀌면 이전·이후 단가와 처리자(토큰의 `userId`), 시각을 `sku_price_history`에 한 행 남긴다. 같은 값을 다시 보내거나 단가 외 필드만 바꾸면 남기지 않는다. 이력 조회 API는 아직 없다.
- 이미 만든 창고 발주·지점 발주·출고 항목의 단가는 스냅샷이라 바뀌지 않는다.

## GET /products/skus — SKU 목록

- STORE_OWNER에게는 매입가(`currentPurchasePrice`)와 안전재고(`safetyStockQuantity`) 미노출.
- Query: `productId`, `brandId`, `categoryId`, `keyword`, `isActive`. 정렬은 고정(등록 일시 `createdAt` 내림차순, 같으면 `skuId` 내림차순).
- 에러: 404 `PRODUCT_NOT_FOUND`(존재하지 않는 `productId` 필터), 404 `BRAND_NOT_FOUND`(`brandId`), 404 `CATEGORY_NOT_FOUND`(`categoryId`)
- 응답 항목에 `optionValues[]` 포함. 응답 `data.items[]`.

## GET /products/skus/{skuId} — SKU 상세

- 상품/브랜드/카테고리 이름, `optionValues[]` 포함. 에러 `SKU_NOT_FOUND` 404.

## POST /products/skus/{skuId}/options — SKU 옵션 연결

- Body: `optionValueIds[]`. 옵션 그룹당 값 1개만 허용. All-or-nothing. 연결 해제 API 없음.
- 에러: `SKU_NOT_FOUND` 404, `OPTION_VALUE_NOT_FOUND` 404, `DUPLICATE_OPTION_VALUE` 409, `OPTION_VALUE_INACTIVE` 409, `OPTION_GROUP_CONFLICT` 409

## PATCH /products/skus/{skuId}/status — SKU 상태 변경 (코드 전용)

- 권한: HQ_ADMIN(마스터 데이터, [ADR-003](../adr/003-master-data-owned-by-hq.md))
- Body: `isActive`(필수, boolean)
- 200. 응답: `skuId, isActive`
- 에러: `SKU_NOT_FOUND` 404, `PRODUCT_INACTIVE` 409(활성화(`isActive=true`)하려는 SKU의 상품이 비활성), `SKU_IN_USE` 409(비활성화(`isActive=false`)하려는 활성 SKU에 재고가 남아 있거나 진행 중 입고·창고 발주·출고·지점 발주가 있음)
- 규칙: 비활성화는 상품 상태와 무관하게 가능하되, 사용 중이면 막힌다(진행 중 상태 기준은 `PRODUCT_IN_USE`와 같다). 이미 비활성인 SKU에 다시 `false`를 보내면 검사 없이 성공한다. 활성화는 상품이 활성일 때만 가능하다.
