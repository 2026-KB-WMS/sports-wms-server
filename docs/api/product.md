# Product API 명세

> 기준은 레포. 2026-10-02 Notion(최종 수정 2026-09-24)에서 이전. 공통 규칙은 [conventions.md](conventions.md) 참고.
> Base path: `/api/v1/products`. 목록 API는 `data.items`만 반환(페이지네이션 보류, ADR/conventions 메모 참고). 404는 도메인별 코드 사용.

## 구현 대비 메모

- 코드에만 있고 Notion 명세가 없는 엔드포인트: `PATCH /api/v1/products/skus/{skuId}/status` (SKU 활성/비활성). 명세 보강 필요.
- Notion 명세의 `pageInfo`, 일반 `NOT_FOUND`는 현재 구현 기준(페이지네이션 보류, 도메인별 404)과 다름 → conventions.md 기준 따름.
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
| GET | /products/categories | 전체 | 카테고리 목록 |
| POST | /products/categories | HQ_ADMIN | 카테고리 등록 |
| POST | /products/option-groups | HQ_ADMIN | 옵션 그룹 등록 |
| POST | /products/option-groups/{optionGroupId}/values | HQ_ADMIN | 옵션 값 등록 |
| GET | /products/{productId}/option-groups | 전체 | 상품의 옵션 그룹 조회 |
| GET | /products/skus | 전체 | SKU 목록 |
| POST | /products/skus | HQ_ADMIN | SKU 등록 |
| GET | /products/skus/{skuId} | 전체 | SKU 상세 |
| POST | /products/skus/{skuId}/options | HQ_ADMIN | SKU 옵션 연결 |
| PATCH | /products/skus/{skuId}/status | (코드 전용) | SKU 상태 변경 — 명세 없음 |

공통 에러: 400 검증 실패, 401 미인증, 403 권한 없음.

## POST /products — 상품 등록 (P0)

- 권한: HQ_ADMIN
- Body: `brandId`, `categoryId`, `productCode`(≤50, unique), `productName`(≤200), `description`(선택)
- 응답: 201, 생성된 상품(status ACTIVE)
- 에러: `BRAND_NOT_FOUND` 404, `CATEGORY_NOT_FOUND` 404, `BRAND_INACTIVE` 409, `CATEGORY_INACTIVE` 409, `DUPLICATE_PRODUCT_CODE` 409

## GET /products — 상품 목록

- 권한: 전체. STORE_OWNER는 활성 상품만 조회.
- Query: `page`, `size`, `sort`, `categoryId`, `brandId`, `keyword`, `isActive`
- 응답: `data.items[]`
- 에러: 400, 401

## GET /products/{productId} — 상품 상세

- 에러: `PRODUCT_NOT_FOUND` 404. STORE_OWNER가 비활성 상품 조회 시에도 404.

## PATCH /products/{productId} — 상품 수정

- 권한: HQ_ADMIN
- Body(부분 수정, 최소 1개 필드): `productName`, `description`, `brandId`, `categoryId`, `isActive`
- `productCode`는 수정 불가(포함 시 400).
- 변경 대상 brand/category는 활성이어야 함, 아니면 409(`BRAND_INACTIVE`/`CATEGORY_INACTIVE`).
- 멱등. 상품 비활성화 시 SKU 연쇄 비활성화는 보류.
- 에러: `PRODUCT_NOT_FOUND`, `BRAND_NOT_FOUND`, `CATEGORY_NOT_FOUND` 404

## GET /products/brands, POST /products/brands

- POST(HQ_ADMIN): `brandName`(≤100, unique), `description`(≤500). 에러 `DUPLICATE_BRAND_NAME` 409. 201.
- GET: query `keyword`, `isActive`, `sort`(기본 `brandName,asc`). 응답 `data.items[]`.

## GET /products/categories, POST /products/categories

- POST(HQ_ADMIN): `parentCategoryId`(선택), `categoryCode`(≤50, unique), `categoryName`(≤100), `sortOrder`(기본 0). `depth`는 서버가 계산.
- 에러: `PARENT_CATEGORY_NOT_FOUND` 404, `DUPLICATE_CATEGORY_CODE` 409, `PARENT_CATEGORY_INACTIVE` 409
- 미결: 카테고리 최대 depth 제한.
- GET: query `parentCategoryId`, `depth`, `keyword`, `isActive`.

## POST /products/option-groups

- Body: `name`(≤100, unique). 에러 `DUPLICATE_OPTION_GROUP_NAME` 409. 상태 컬럼 없음.

## POST /products/option-groups/{optionGroupId}/values

- Body: `value`(≤100, 그룹 내 unique), `sortOrder`
- 에러: `OPTION_GROUP_NOT_FOUND` 404, `DUPLICATE_OPTION_VALUE` 409

## GET /products/{productId}/option-groups

- 페이지네이션 없이 전체 반환. 에러 `PRODUCT_NOT_FOUND` 404.

## POST /products/skus — SKU 등록

- 권한: HQ_ADMIN
- Body: `productId`, `skuCode`(≤50), `barcode`(선택, ≤100, unique), `skuName`(≤200), `weight`(선택), `currentPurchasePrice`(선택), `currentSupplyPrice`(선택), `unit`(기본 `EA`), `safetyStockQuantity`(기본 0, BIGINT)
- 에러: `PRODUCT_NOT_FOUND` 404, `PRODUCT_INACTIVE` 409, `DUPLICATE_SKU_CODE` 409, `DUPLICATE_BARCODE` 409
- 미결: 동일 옵션 조합 SKU 중복 허용 규칙.

## GET /products/skus — SKU 목록

- STORE_OWNER에게는 매입가(`currentPurchasePrice`)와 안전재고(`safetyStockQuantity`) 미노출.
- 응답 항목에 `optionValues[]` 포함. 응답 `data.items[]`.

## GET /products/skus/{skuId} — SKU 상세

- 상품/브랜드/카테고리 이름, `optionValues[]` 포함. 에러 `SKU_NOT_FOUND` 404.

## POST /products/skus/{skuId}/options — SKU 옵션 연결

- Body: `optionValueIds[]`. 옵션 그룹당 값 1개만 허용. All-or-nothing. 연결 해제 API 없음.
- 에러: `SKU_NOT_FOUND` 404, `OPTION_VALUE_NOT_FOUND` 404, `DUPLICATE_OPTION_VALUE` 409, `OPTION_VALUE_INACTIVE` 409, `OPTION_GROUP_CONFLICT` 409
