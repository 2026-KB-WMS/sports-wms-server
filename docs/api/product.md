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
- 카테고리별 옵션 그룹 지정·상품 스펙·SKU 옵션 검증(#241, [ADR-025](../adr/025-category-option-groups-spec-and-sku-options.md)): 카테고리마다 옵션 그룹을 `SPEC`(상품 단위) 또는 `SKU`(SKU 단위)로 지정하고, 상품 등록은 SPEC 값을, SKU 등록은 SKU 옵션 값을 지정된 그룹마다 하나씩 받는다. 같은 상품에 같은 옵션 조합의 SKU는 둘 수 없다(409). 이 규칙은 등록·수정 시점에만 적용하며 이미 저장된 상품·SKU는 바꾸지 않는다.
- 기준 데이터 시드(#241): 마이그레이션 V23이 카테고리 8개(`RKT` 라켓, `SHC` 셔틀콕, `SHO` 신발, `APR` 의류, `BAG` 가방, `STR` 스트링, `GRP` 그립, `PRT` 보호대), 옵션 그룹 10개와 값, 카테고리별 SPEC·SKU 지정을 넣는다. 같은 코드·이름이 이미 있으면 건너뛴다. 마이그레이션 V24는 브랜드 12개(`YNX` 요넥스, `VCT` 빅터, `LNG` 리닝, `MZN` 미즈노, `BBL` 바볼랏, `WLS` 윌슨, `HED` 헤드, `NIK` 나이키, `ADS` 아디다스, `ASC` 아식스, `KWS` 가와사키, `PMA` 푸마)를 같은 방식으로 넣는다.
- 미결: 카테고리 최대 depth.

## 엔드포인트 목록 (15 + 코드 전용 5)

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
| PATCH | /products/categories/{categoryId} | HQ_ADMIN | 카테고리 수정(이름·정렬 순서) |
| GET | /products/categories/{categoryId}/option-groups | 전체 | 카테고리에 적용되는 옵션 그룹(상속 포함) (코드 전용, ADR-025) |
| PUT | /products/categories/{categoryId}/option-groups | HQ_ADMIN | 카테고리의 옵션 그룹 지정 교체 (코드 전용, ADR-025) |
| GET | /products/option-groups | 전체 | 옵션 그룹 전체 목록(값 포함) (코드 전용, ADR-025) |
| POST | /products/option-groups | HQ_ADMIN | 옵션 그룹 등록 |
| POST | /products/option-groups/{optionGroupId}/values | HQ_ADMIN | 옵션 값 등록 |
| GET | /products/{productId}/option-groups | 전체 | 상품의 옵션 그룹 조회 |
| GET | /products/skus | 전체 | SKU 목록 |
| POST | /products/skus | HQ_ADMIN | SKU 등록 |
| GET | /products/skus/units | 전체 | SKU 수량 단위 허용 목록 (코드 전용, ADR-024) |
| GET | /products/skus/{skuId} | 전체 | SKU 상세 |
| POST | /products/skus/{skuId}/options | HQ_ADMIN | SKU 옵션 연결 |
| PATCH | /products/skus/{skuId}/status | HQ_ADMIN | SKU 활성/비활성 변경 (코드 전용, Notion 명세 없음) |

공통 에러: 400 검증 실패, 401 미인증, 403 권한 없음.

## POST /products — 상품 등록 (P0)

- 권한: HQ_ADMIN
- Body: `brandId`, `categoryId`, `productCode`(≤50, unique), `productName`(≤200), `description`(선택), `specOptionValueIds`(선택, 카테고리에 `SPEC`으로 지정된 옵션 그룹마다 값 하나씩)
- `SPEC` 그룹이 지정된 카테고리는 그 그룹의 값을 모두 보내야 한다. 값은 활성이어야 하고, 지정되지 않았거나 용도가 `SKU`인 그룹의 값, 같은 그룹의 값 둘 이상, 중복 ID는 거절한다. 지정된 `SPEC` 그룹이 없으면 생략해도 된다.
- 응답: 201, 생성된 상품(status ACTIVE). `specs[]`(`optionGroupId, optionGroupName, optionValueId, optionCode, value`)에 지정된 스펙이 담긴다(없으면 빈 배열).
- 에러: `BRAND_NOT_FOUND` 404, `CATEGORY_NOT_FOUND` 404, `OPTION_VALUE_NOT_FOUND` 404, `BRAND_INACTIVE` 409, `CATEGORY_INACTIVE` 409, `DUPLICATE_PRODUCT_CODE` 409, `OPTION_VALUE_INACTIVE` 409, `OPTION_GROUP_CONFLICT` 409, `DUPLICATE_OPTION_VALUE` 409(같은 ID 중복), 400 `OPTION_NOT_ALLOWED_FOR_CATEGORY`(지정되지 않았거나 용도가 다른 그룹의 값), 400 `REQUIRED_OPTION_MISSING`(지정된 `SPEC` 그룹의 값 누락)

## GET /products — 상품 목록

- 권한: 전체. STORE_OWNER는 활성 상품만 조회.
- Query: `categoryId`, `brandId`, `keyword`, `isActive`. `page`·`size`·`sort`는 받지 않는다(페이지네이션 보류).
- 정렬은 고정(등록 일시 `createdAt` 내림차순, 같으면 `productId` 내림차순).
- 응답: `data.items[]`
- 에러: 400, 401, 404 `BRAND_NOT_FOUND`(존재하지 않는 `brandId` 필터), 404 `CATEGORY_NOT_FOUND`(존재하지 않는 `categoryId` 필터)

## GET /products/{productId} — 상품 상세

- 에러: `PRODUCT_NOT_FOUND` 404. STORE_OWNER가 비활성 상품 조회 시에도 404.
- 응답에 `specs[]`가 포함된다(등록 응답과 같은 형식, 목록 API에는 없음).

## PATCH /products/{productId} — 상품 수정

- 권한: HQ_ADMIN
- Body(부분 수정, 최소 1개 필드): `productName`, `description`, `brandId`, `categoryId`, `isActive`, `specOptionValueIds`
- `productCode`는 수정 불가(포함 시 400).
- 변경 대상 brand/category는 활성이어야 함, 아니면 409(`BRAND_INACTIVE`/`CATEGORY_INACTIVE`).
- `specOptionValueIds`를 보내면 스펙 값 **전체를 이 목록으로 교체**한다(빈 목록도 교체이며, 필수 `SPEC` 그룹이 있으면 400). 등록과 같은 검증을 거친다. 보내지 않으면 스펙은 그대로다.
- **스펙이나 SKU가 있는 상품의 카테고리는 바꿀 수 없다**(409 `PRODUCT_CATEGORY_LOCKED`). 둘 다 없는 상품의 카테고리를 바꾸면 새 카테고리의 `SPEC` 그룹을 채우도록 `specOptionValueIds`를 함께 보내야 한다. 같은 카테고리를 다시 보내는 요청은 검사하지 않는다.
- 멱등. 상품 비활성화(`isActive=false`) 시 하위 ACTIVE SKU를 같은 트랜잭션에서 함께 비활성화한다(재고 도메인은 SKU 상태만 보기 때문). 이미 비활성인 SKU는 그대로 두며, 상품을 다시 활성화(`isActive=true`)해도 SKU는 자동으로 활성화되지 않는다.
- 비활성화(`isActive=false`)는 재고가 남아 있거나 진행 중인 입고·창고 발주·출고·지점 발주가 있으면 409 `PRODUCT_IN_USE`이고 상품·SKU 상태는 바뀌지 않는다. 이미 비활성인 상품에 다시 `false`를 보내는 요청은 검사 없이 기존처럼 성공한다.
- 에러: `PRODUCT_NOT_FOUND`, `BRAND_NOT_FOUND`, `CATEGORY_NOT_FOUND`, `OPTION_VALUE_NOT_FOUND` 404, `BRAND_INACTIVE`·`CATEGORY_INACTIVE`·`PRODUCT_IN_USE`·`PRODUCT_CATEGORY_LOCKED`·`OPTION_VALUE_INACTIVE`·`OPTION_GROUP_CONFLICT`·`DUPLICATE_OPTION_VALUE` 409, 400 `OPTION_NOT_ALLOWED_FOR_CATEGORY`·`REQUIRED_OPTION_MISSING`

## GET /products/brands, POST /products/brands

- POST(HQ_ADMIN): `brandCode`(**필수**, 영문 3자, 직접 입력, 서버가 대문자로 저장, unique), `brandName`(≤100, unique), `description`(≤500). 에러 400(코드 누락·형식 위반), `DUPLICATE_BRAND_NAME` 409, `DUPLICATE_BRAND_CODE` 409(DB unique 위반도 동일 매핑). 201. 응답: `brandId, brandCode, brandName, description, isActive, createdAt`. ([ADR-023](../adr/023-brand-code.md))
- GET: query `keyword`(브랜드명·코드 부분 일치, 대소문자 무시), `isActive`. 응답 항목: `brandId, brandCode, brandName, description, isActive`. 코드 도입 전 브랜드는 `brandCode`가 `null`이다. 정렬은 고정(`brandName` 오름차순, 같으면 `brandId` 오름차순). 응답 `data.items[]`.

## PATCH /products/brands/{brandId} — 브랜드 수정 (#216)

- 권한: HQ_ADMIN
- Body(부분 수정, 최소 1개 필드): `brandName`(≤100, 공백 불가), `description`(≤500), `brandCode`(영문 3자). 값이 있는 필드만 바꾼다.
- `brandCode`는 **코드가 없는 기존 브랜드에 한 번만** 지정할 수 있다. 이미 코드가 있으면 400이다.
- 응답: `brandId, brandCode, brandName, description, isActive`(목록 항목과 같음).
- 에러: 400(필드 없음, 이름 공백·초과, 설명 초과, 코드 형식 위반, 이미 코드가 있는 브랜드의 코드 변경), 403, 404 `BRAND_NOT_FOUND`, 409 `DUPLICATE_BRAND_NAME`(다른 브랜드가 쓰는 이름. 자기 이름 그대로는 통과), 409 `DUPLICATE_BRAND_CODE`
- 비활성 브랜드도 수정할 수 있고 활성 상태는 바뀌지 않는다. 이미 연결된 상품은 영향이 없다.
- 브랜드·카테고리의 **비활성화·재활성화 API는 두지 않는다**(2026-10-10 결정, 필요한 업무가 없음).

## GET /products/categories, POST /products/categories

- POST(HQ_ADMIN): `parentCategoryId`(선택), `categoryCode`(**영문 3자**, 서버가 대문자로 저장, unique), `categoryName`(≤100), `sortOrder`(기본 0). `depth`는 서버가 계산.
- 201. 응답(POST·GET 공통 항목): `categoryId, parentCategoryId, categoryCode, categoryName, depth, sortOrder, isActive, createdAt`
- 에러: 400(코드 누락·영문 3자 위반), `PARENT_CATEGORY_NOT_FOUND` 404, `DUPLICATE_CATEGORY_CODE` 409, `PARENT_CATEGORY_INACTIVE` 409
- 코드 규칙은 브랜드 코드와 같다([ADR-023](../adr/023-brand-code.md), [ADR-025](../adr/025-category-option-groups-spec-and-sku-options.md)). 영문 3자 규칙 도입 전에 만든 카테고리의 코드는 그대로 남고 수정할 수 없다.
- 미결: 카테고리 최대 depth 제한.
- GET: query `parentCategoryId`, `depth`, `keyword`, `isActive`. 정렬은 고정(`sortOrder` 오름차순, 같으면 `categoryId` 오름차순). 에러: 404 `CATEGORY_NOT_FOUND`(존재하지 않는 `parentCategoryId` 필터).

## PATCH /products/categories/{categoryId} — 카테고리 수정 (#216)

- 권한: HQ_ADMIN
- Body(부분 수정, 최소 1개 필드): `categoryName`(≤100, 공백 불가), `sortOrder`(≥0). 값이 있는 필드만 바꾼다.
- `categoryCode`와 `parentCategoryId`는 수정할 수 없다(포함 시 400). 상위를 옮기면 하위 카테고리의 `depth`가 모두 바뀌어야 해서 지원하지 않는다.
- 응답: 목록 항목과 같은 필드(`categoryId, parentCategoryId, categoryCode, categoryName, depth, sortOrder, isActive, createdAt`).
- 에러: 400, 403, 404 `CATEGORY_NOT_FOUND`
- 비활성 카테고리도 수정할 수 있고 활성 상태는 바뀌지 않는다. 카테고리 비활성화 API는 두지 않는다(위 브랜드 결정과 같음).

## GET /products/option-groups — 옵션 그룹 전체 목록 (코드 전용)

- 권한: 로그인한 모든 역할. 페이지네이션 없음. 정렬은 고정(옵션 그룹 ID 오름차순, 값은 `sortOrder` 오름차순).
- 200. `data.items[]`: `optionGroupId`, `name`, `values[]`(`optionValueId`, `optionCode`, `value`, `sortOrder`, `isActive`). 비활성 값도 포함하며 `optionCode`는 코드 도입 전 값이면 `null`이다.

## PUT·GET /products/categories/{categoryId}/option-groups — 카테고리별 옵션 그룹 (코드 전용, #241)

- PUT 권한: HQ_ADMIN. GET: 로그인한 모든 역할.
- PUT Body: `items[]`(`optionGroupId`, `optionType`(`SPEC` 또는 `SKU`), `sortOrder`(기본 0)). 이 카테고리 **자신의 지정 전체를 목록으로 교체**한다. 빈 목록이면 자신의 지정을 모두 지운다. 같은 그룹을 두 번 넣으면 400이다.
- 지정은 하위 카테고리가 상속한다. 하위에서 같은 그룹을 다시 지정하면 가까운 카테고리의 지정(용도·순서)이 우선한다.
- 응답(PUT·GET 공통): 200, `data.items[]`는 **상속을 합친 적용 결과**이며 `sortOrder` 오름차순(같으면 그룹 ID 오름차순)이다. 항목: `optionGroupId`, `name`, `optionType`, `sortOrder`, `definedCategoryId`(지정을 등록한 카테고리), `inherited`(상위에서 상속했으면 `true`), `values[]`(`optionValueId`, `optionCode`, `value`, `sortOrder`, `isActive`).
- 상품 등록·SKU 등록은 이 적용 결과를 기준으로 검증한다. 지정을 바꿔도 이미 저장된 상품·SKU는 바뀌지 않는다.
- 에러: 400 `VALIDATION_ERROR`(`items` 누락, `optionType` 누락·허용 값 아님, 같은 그룹 중복, 음수 `sortOrder`), 403, 404 `CATEGORY_NOT_FOUND`, 404 `OPTION_GROUP_NOT_FOUND`

## POST /products/option-groups

- Body: `name`(≤100, unique). 에러 `DUPLICATE_OPTION_GROUP_NAME` 409. 상태 컬럼 없음.
- 옵션 그룹 자체에는 SPEC·SKU 구분이 없다. 용도는 카테고리에 지정할 때 정해진다(아래 카테고리별 옵션 그룹).

## POST /products/option-groups/{optionGroupId}/values

- Body: `optionCode`(**필수**, 영문·숫자 1~10자, 서버가 대문자로 저장, 그룹 내 unique), `value`(≤100, 그룹 내 unique), `sortOrder`
- `optionCode`는 SKU 코드 자동 생성에 쓴다. 코드 도입 전에 만든 값은 `null`이며 수정 API는 없다(코드가 없는 값을 고르면 SKU 코드를 직접 입력해야 한다).
- 응답: `optionValueId, optionGroupId, optionCode, value, sortOrder, isActive, createdAt`
- 에러: 400 `VALIDATION_ERROR`(코드 누락·형식 위반), `OPTION_GROUP_NOT_FOUND` 404, `DUPLICATE_OPTION_VALUE` 409, `DUPLICATE_OPTION_CODE` 409(같은 그룹의 같은 코드)

## GET /products/{productId}/option-groups

- 페이지네이션 없이 전체 반환. 에러 `PRODUCT_NOT_FOUND` 404. 값 항목에 `optionCode`, `isActive`가 포함된다.

## POST /products/skus — SKU 등록

- 권한: HQ_ADMIN
- Body: `productId`, `skuCode`(선택, ≤50), `barcode`(선택, ≤100, unique), `skuName`(선택, ≤200), `weight`(선택, 물류용 실측 중량이며 옵션의 "무게 등급"과 다른 값), `currentPurchasePrice`(**필수**, 0 이상), `currentSupplyPrice`(**필수**, 0 이상), `unit`(`EA` / `BOX` / `SET` 중 하나, 기본 `EA`), `safetyStockQuantity`(기본 0, BIGINT), `optionValueIds`(카테고리에 `SKU`로 지정된 옵션 그룹마다 값 하나씩)
- **옵션 검증**([ADR-025](../adr/025-category-option-groups-spec-and-sku-options.md)): 상품 카테고리(상위 상속 포함)에 `SKU`로 지정된 그룹의 값을 모두 보내야 하고, 등록과 같은 트랜잭션에서 SKU와 옵션 연결을 저장한다. 지정된 `SKU` 그룹이 없으면 `optionValueIds`를 생략해도 된다.
- **조합 중복**: 같은 상품에 같은 옵션 값 조합의 SKU(비활성 포함)가 이미 있으면 409 `DUPLICATE_SKU_OPTION_COMBINATION`이다. 옵션이 없는 SKU끼리도 같은 조합으로 본다.
- **코드·이름 자동 생성**: `skuCode`를 생략하면 `상품코드-옵션코드-옵션코드`(카테고리 지정 순서)로, `skuName`을 생략하면 `상품명 (옵션값/옵션값)`으로 만든다. 옵션이 없으면 상품 코드·상품명 그대로다. 선택한 값에 `optionCode`가 없거나 결과가 50자(코드)·200자(이름)를 넘으면 400이므로 직접 입력한다. 직접 입력한 값은 그대로 쓴다(공백만이면 400).
- 응답: 201. `optionValues[]`에 연결된 옵션이 담긴다.
- `unit`은 서버 허용 목록([ADR-024](../adr/024-sku-unit-allowlist.md))과 정확히 일치해야 한다. 목록 밖 값(소문자·한글·빈 문자열 포함)은 400이다. 허용 목록은 `GET /products/skus/units`로 조회한다.
- 에러: 400 `VALIDATION_ERROR`(단가 누락·음수, 허용되지 않은 `unit`, 자동 생성 불가·길이 초과 포함), 400 `OPTION_NOT_ALLOWED_FOR_CATEGORY`·`REQUIRED_OPTION_MISSING`, `PRODUCT_NOT_FOUND`·`OPTION_VALUE_NOT_FOUND` 404, `PRODUCT_INACTIVE`·`OPTION_VALUE_INACTIVE`·`OPTION_GROUP_CONFLICT`·`DUPLICATE_OPTION_VALUE`·`DUPLICATE_SKU_OPTION_COMBINATION`·`DUPLICATE_SKU_CODE`·`DUPLICATE_BARCODE` 409

## GET /products/skus/units — SKU 수량 단위 허용 목록 (코드 전용)

- 권한: 로그인한 모든 역할. 페이지네이션 없음.
- 200. `data.items[]`: `code`(`EA`, `BOX`, `SET`), `name`(개, 박스, 세트). 등록 요청의 `unit`에는 `code`를 보낸다.
- 코드 도입 전에 저장된 SKU의 `unit`이 목록 밖 값이어도 변환하지 않는다. `unit`은 SKU 수정 API 대상이 아니므로 그대로 남는다.

## PATCH /products/skus/{skuId} — SKU 수정 (코드 추가, #207)

- 권한: HQ_ADMIN(마스터 데이터, [ADR-003](../adr/003-master-data-owned-by-hq.md)). 비활성 SKU도 수정할 수 있다.
- Body: 모든 필드가 선택이며 값이 있는 필드만 수정한다. **최소 1개 필요**(없으면 400).

| 필드 | 규칙 |
|---|---|
| skuName | 1~200자 |
| barcode | ≤100, 다른 SKU와 겹치면 409 `DUPLICATE_BARCODE`(자기 바코드를 다시 보내는 것은 허용). 비우기는 지원하지 않음 |
| weight | 0 이상(물류용 실측 중량, 단위는 운영 기준을 따른다) |
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
- **옵션이 없는 기존 SKU에 한 번만** 연결할 수 있다(이미 옵션이 있으면 409 `SKU_OPTIONS_ALREADY_CONNECTED`). SKU 등록과 같은 검증(카테고리 지정·필수 그룹·조합 중복)을 거치며, 새 SKU는 등록 요청의 `optionValueIds`로 함께 연결한다.
- 에러: `SKU_NOT_FOUND` 404, `PRODUCT_NOT_FOUND`·`OPTION_VALUE_NOT_FOUND` 404, `DUPLICATE_OPTION_VALUE` 409, `OPTION_VALUE_INACTIVE` 409, `OPTION_GROUP_CONFLICT` 409, `SKU_OPTIONS_ALREADY_CONNECTED` 409, `DUPLICATE_SKU_OPTION_COMBINATION` 409, 400 `OPTION_NOT_ALLOWED_FOR_CATEGORY`·`REQUIRED_OPTION_MISSING`

## PATCH /products/skus/{skuId}/status — SKU 상태 변경 (코드 전용)

- 권한: HQ_ADMIN(마스터 데이터, [ADR-003](../adr/003-master-data-owned-by-hq.md))
- Body: `isActive`(필수, boolean)
- 200. 응답: `skuId, isActive`
- 에러: `SKU_NOT_FOUND` 404, `PRODUCT_INACTIVE` 409(활성화(`isActive=true`)하려는 SKU의 상품이 비활성), `SKU_IN_USE` 409(비활성화(`isActive=false`)하려는 활성 SKU에 재고가 남아 있거나 진행 중 입고·창고 발주·출고·지점 발주가 있음)
- 규칙: 비활성화는 상품 상태와 무관하게 가능하되, 사용 중이면 막힌다(진행 중 상태 기준은 `PRODUCT_IN_USE`와 같다). 이미 비활성인 SKU에 다시 `false`를 보내면 검사 없이 성공한다. 활성화는 상품이 활성일 때만 가능하다.
