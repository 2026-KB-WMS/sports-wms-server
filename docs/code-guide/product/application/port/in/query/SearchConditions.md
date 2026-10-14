# 검색 조건 (SearchCondition)

`product/application/port/in/query/` · 목록 조회의 필터 `record`.

## 흐름

`쿼리 파라미터` → 컨트롤러가 `new XxxSearchCondition(...)` → `UseCase.getXxx(condition)` → 서비스가 참조 대상 존재 확인 → `Repository.search(condition)` → 어댑터가 JPQL 파라미터로 변환

## 공통 규칙

- **null인 조건은 무시**한다 (JPQL의 `:param is null or ...`).
- `isActive`는 `ProductStatus.fromActiveFlag`로 ACTIVE/INACTIVE로 바뀌고, null이면 상태 조건이 없다.
- `keyword`는 `SearchKeyword.normalize`로 정리한 뒤 대소문자 무시 부분 일치로 쓴다.
- 점주(STORE_OWNER) 요청이면 서비스가 `isActive=true`로 바꾼 **새 조건 객체**를 만들어 쓴다. 점주가 `isActive=false`를 보내도 활성만 보인다.

## BrandSearchCondition

| 항목 | 내용 |
|---|---|
| 용도 | 브랜드 목록을 필터링한다 |
| 엔드포인트 | GET `/api/v1/products/brands` |
| 만드는 곳 | `BrandController.getBrands` |
| 쓰는 곳 | `BrandService.getBrands` → `BrandRepository.search` |
| 정렬 | 브랜드명 오름차순 |

| 필드 | 설명 |
|---|---|
| keyword | 브랜드명 **및 브랜드 코드** 부분 일치 (JPQL은 두 컬럼을 모두 본다) |
| isActive | 활성 상태 필터 |

## CategorySearchCondition

| 항목 | 내용 |
|---|---|
| 용도 | 카테고리 목록을 필터링한다. 트리를 한 단계씩 내려가며 볼 때 `parentCategoryId`를 쓴다 |
| 엔드포인트 | GET `/api/v1/products/categories` |
| 만드는 곳 | `CategoryController.getCategories` |
| 쓰는 곳 | `CategoryService.getCategories` → `CategoryRepository.search` |
| 정렬 | `sortOrder`, 같으면 `categoryId` 오름차순 |
| 검증 | `parentCategoryId`가 있으면 그 카테고리가 없을 때 `CATEGORY_NOT_FOUND` |

| 필드 | 설명 |
|---|---|
| parentCategoryId | 이 카테고리의 **직속** 하위만 |
| depth | 계층 깊이 (1 = 최상위) |
| keyword | 카테고리명·카테고리 코드 부분 일치 |
| isActive | 활성 상태 필터 |

## ProductSearchCondition

| 항목 | 내용 |
|---|---|
| 용도 | 상품 목록을 필터링한다 |
| 엔드포인트 | GET `/api/v1/products` |
| 만드는 곳 | `ProductController.getProducts` |
| 쓰는 곳 | `ProductService.getProducts(condition)` 또는 `(condition, actor)` → `ProductRepository.search` |
| 정렬 | 생성 일시 내림차순, 같으면 `productId` 내림차순 |
| 검증 | `brandId`·`categoryId`가 있으면 존재하지 않을 때 `BRAND_NOT_FOUND`·`CATEGORY_NOT_FOUND` |

| 필드 | 설명 |
|---|---|
| brandId | 브랜드 |
| categoryId | 카테고리 (그 카테고리 **직접 소속**만. 하위 카테고리 상품은 포함하지 않는다) |
| keyword | 상품명·상품 코드 부분 일치 |
| isActive | 활성 상태 필터 |

## ProductSkuSearchCondition

| 항목 | 내용 |
|---|---|
| 용도 | SKU 목록을 필터링한다 |
| 엔드포인트 | GET `/api/v1/products/skus` |
| 만드는 곳 | `ProductSkuController.getSkus` |
| 쓰는 곳 | `ProductSkuService.getSkus(condition)` 또는 `(condition, actor)` → `ProductSkuRepository.search` |
| 정렬 | 생성 일시 내림차순, 같으면 `skuId` 내림차순 |
| 검증 | `productId`·`brandId`·`categoryId`가 있으면 존재하지 않을 때 각각 `PRODUCT_NOT_FOUND`·`BRAND_NOT_FOUND`·`CATEGORY_NOT_FOUND` |

| 필드 | 설명 |
|---|---|
| productId | 이 상품의 SKU만 |
| brandId | SKU가 속한 **상품의** 브랜드 (상품과 조인) |
| categoryId | SKU가 속한 **상품의** 카테고리 (상품과 조인) |
| keyword | SKU 코드·바코드·SKU명 부분 일치 |
| isActive | **SKU** 활성 상태 필터 (상품 상태가 아니다) |
