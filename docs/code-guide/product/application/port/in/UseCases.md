# 인바운드 포트 (UseCase)

`product/application/port/in/` · 컨트롤러가 호출하는 인터페이스. 구현은 `application/service/`의 서비스다. 상세 동작은 각 서비스 문서를 본다.

| UseCase | 구현 | 메서드 | 비고 |
|---|---|---|---|
| `BrandQueryUseCase` | `BrandService` | `registerBrand`, `updateBrand`, `getBrands`, `getBrand` | 이름이 Query지만 등록·수정도 포함 |
| `CategoryUseCase` | `CategoryService` | `registerCategory`, `updateCategory`, `getCategories`, `getCategory` | |
| `CategoryOptionGroupUseCase` | `CategoryOptionGroupService` | `assignOptionGroups`, `getOptionGroups` | 지정은 통째로 교체, 조회는 상속 반영 |
| `OptionGroupUseCase` | `OptionGroupService` | `registerOptionGroup`, `getOptionGroups`, `getOptionGroupsByProduct` | |
| `OptionValueUseCase` | `OptionValueService` | `registerOptionValue` | |
| `ProductUseCase` | `ProductService` | `registerProduct`, `updateProduct`, `getProducts`(2종), `getProduct`(2종), `getProductSpecs` | `actor`를 받는 오버로드는 사용자 요청용(점주 필터) |
| `ProductSkuUseCase` | `ProductSkuService` | `registerSku`, `updateSku`, `getSkus`(2종), `getSku`(2종), `changeSkuStatus`, `connectOptions`, `getSkuOptions`, `getSkuOptionsBySkuIds` | `actor` 없는 조회는 다른 도메인의 내부 조회용 |
| `ProductCodeUseCase` | `ProductCodeService` | `getSkuUnits` | 고정 코드 목록 |

## 하위 패키지

- `command/`: 쓰기 요청 입력 객체 (`...RegisterCommand`, `...UpdateCommand`, `...AssignCommand`, `...ConnectCommand`)
- `query/`: 검색 조건 (`...SearchCondition`)
- `result/`: 서비스가 돌려주는 요약 (`...Summary`, `CodeItem`)
