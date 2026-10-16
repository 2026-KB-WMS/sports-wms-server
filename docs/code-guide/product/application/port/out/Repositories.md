# 아웃바운드 포트 (Repository)

`product/application/port/out/` · 서비스가 의존하는 영속성 인터페이스. 구현은 `adapter/out/persistence/`의 `*PersistenceAdapter`.

## BrandRepository

| 메서드 | 설명 |
|---|---|
| `save(brand)` | 저장 |
| `findById(brandId)` | 단건 조회 |
| `findByIdForUpdate(brandId)` | **비관적 락**으로 조회. 상품 코드 채번을 브랜드 단위로 직렬화할 때 사용 |
| `search(condition)` | 브랜드명 오름차순 |
| `existsByName(name)` / `existsByBrandCode(code)` | 중복 확인 |
| `existsByBrandCodeAndBrandIdNot(code, excludeId)` / `existsByNameAndBrandIdNot(name, excludeId)` | 자기 자신 제외 중복 확인 |

## CategoryRepository

| 메서드 | 설명 |
|---|---|
| `save`, `findById` | 저장, 단건 조회 |
| `search(condition)` | `sortOrder` 오름차순 |
| `existsByCategoryCode(code)` | 코드 중복 확인 |

## CategoryOptionGroupRepository

| 메서드 | 설명 |
|---|---|
| `findByCategoryIds(ids)` | 여러 카테고리의 지정을 한 번에 조회 |
| `replaceAll(categoryId, mappings)` | 카테고리의 지정을 전부 지우고 주어진 목록으로 교체 |

## OptionGroupRepository

| 메서드 | 설명 |
|---|---|
| `save`, `findById`, `findAll` | 기본 CRUD |
| `findAllByIds(ids)` | 한 번에 조회. 없는 ID는 결과에서 빠진다 |
| `existsByName(name)` | 이름 중복 확인 |

## OptionValueRepository

| 메서드 | 설명 |
|---|---|
| `save`, `findById` | 기본 |
| `findAllByIds(ids)` | 한 번에 조회. 없는 ID는 빠진다 |
| `findByOptionGroupId(groupId)` | 그룹의 옵션 값 |
| `existsByOptionGroupIdAndValue` / `existsByOptionGroupIdAndOptionCode` | 그룹 내 값·코드 중복 확인 |

## ProductRepository

| 메서드 | 설명 |
|---|---|
| `save`, `findById` | 기본 |
| `search(condition, page)` | `PageResult` 반환. 조건이 null이면 무시. 정렬은 페이지 조건을 따른다(기본 생성 일시 내림차순) |
| `existsByProductCode(code)` | 코드 중복 확인 |

## ProductSkuRepository

| 메서드 | 설명 |
|---|---|
| `save`, `findById` | 기본 |
| `findAll(productId)` | productId가 null이면 전체 |
| `search(condition, page)` | `PageResult` 반환. 조건이 null이면 무시. 브랜드·카테고리는 SKU가 속한 상품 기준. 정렬은 페이지 조건을 따른다(기본 생성 일시 내림차순) |
| `existsBySkuCode`, `existsByBarcode` | 중복 확인 |

## ProductSpecRepository

| 메서드 | 설명 |
|---|---|
| `findByProductId`, `existsByProductId` | 조회·존재 확인 |
| `replaceAll(productId, optionValueIds)` | 상품 스펙을 전부 지우고 교체 |

## SkuOptionValueRepository

| 메서드 | 설명 |
|---|---|
| `save` | 연결 저장 |
| `findBySkuId`, `findBySkuIdIn(ids)` | 한 SKU / 여러 SKU의 연결(목록 N+1 방지) |
| `existsBySkuIdAndOptionValueId` | 연결 존재 확인 |

## SkuPriceHistoryRepository

| 메서드 | 설명 |
|---|---|
| `save(history)` | 단가 변경 이력 저장 |

## ProductCodeSequenceRepository

| 메서드 | 설명 |
|---|---|
| `next(brandId, categoryId)` | 조합의 다음 일련번호. 같은 번호를 두 번 주지 않지만, 호출자가 **브랜드 행을 먼저 잠가야** 한다 |

## ProductUsagePort

상품·SKU가 재고나 진행 중 업무에서 쓰이는지 확인하는 읽기 전용 포트. 진행 중 업무는 소유 도메인의 `*UsageUseCase`가 판단하고, 재고 존재만 조회 조인(ADR-007)이다. 비활성화 전에 사용.

| 메서드 | 판단 기준 |
|---|---|
| `hasStock(productId)` | 상품의 어느 SKU든 보유·할당 수량이 0보다 큰 재고 행 |
| `hasInProgressInbounds(productId)` | 입고 상태 ARRIVED·INSPECTING 항목에 포함 |
| `hasInProgressPurchaseOrders(productId)` | 창고 발주 상태 REQUESTED·CONFIRMED 항목에 포함 |
| `hasInProgressOutbounds(productId)` | 출고 상태 READY·PICKING·PICKED·SHIPPED이고 그 지점 발주 항목에 포함 |
| `hasInProgressStoreOrders(productId)` | 지점 발주 상태 REQUESTED·APPROVED·ASSIGNED·ON_HOLD 항목에 포함 |
| `isSkuInUse(skuId)` | 위 다섯 조건 중 하나라도 해당(SKU 하나 기준) |
