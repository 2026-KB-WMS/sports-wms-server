# product 코드 가이드

`src/main/java/com/kb/wms/product/`의 패키지 구조를 그대로 따라 클래스별 설명을 모았다. 같은 폴더의 `.md`가 같은 폴더의 `.java`에 대응한다. 필드만 있거나 한 줄짜리 클래스는 패키지 단위 md 하나로 묶었다.

## 읽는 순서 추천

1. `domain/entity/` → `domain/enums/` : 상품 도메인의 모델과 규칙
2. `application/port/in/UseCases.md` → `application/service/` : 무엇을 하고 어떻게 검증하는지
3. `adapter/in/web/` : API 엔드포인트와 응답 조립
4. `application/port/out/Repositories.md` → `adapter/out/persistence/` : 저장 방식

## 문서 위치

| 패키지 | 문서 |
|---|---|
| `domain/entity` | 클래스별 md (Brand, Category, CategoryOptionGroup, OptionGroup, OptionValue, Product, ProductSku, ProductSpec, SkuOptionValue, SkuPriceHistory) |
| `domain/enums` | `Enums.md` (OptionType, ProductStatus, SkuUnit) |
| `exception` | `ProductErrorCode.md` (ProductErrorCode, ProductConstraintErrorCodes) |
| `application/service` | 클래스별 md (서비스 7개, Resolver 2개, CodeIssuer 1개, ProductCodeService 1개) |
| `application/port/in` | `UseCases.md`, `command/Commands.md`, `query/SearchConditions.md`, `result/Results.md` |
| `application/port/out` | `Repositories.md` |
| `adapter/in/web` | 컨트롤러별 md, `dto/request/Requests.md`, `dto/response/Responses.md` |
| `adapter/out/persistence` | `PersistenceAdapters.md`, `repository/JpaRepositories.md`, `entity/JpaEntities.md` |

## 한눈에 보는 핵심 규칙

- **코드는 서버가 만든다**: 상품 코드 `브랜드-카테고리-0001`, SKU 코드 `상품코드-옵션코드...` (ADR-026)
- **옵션은 카테고리가 정한다**: 카테고리에 옵션 그룹을 SPEC·SKU 용도로 지정하고 하위가 상속한다 (ADR-025)
- **비활성화 전에 사용 여부를 본다**: 재고·진행 중 업무가 있으면 `PRODUCT_IN_USE` / `SKU_IN_USE`
- **상품을 비활성화하면 하위 SKU도 같이 비활성화**된다
- **점주(STORE_OWNER)**: 활성 상품·SKU만 보고, 매입 단가와 안전 재고는 응답에서 빠진다
- **브랜드·카테고리는 잠금 조건이 있다**: SKU가 있으면 브랜드 변경 불가, SKU나 스펙이 있으면 카테고리 변경 불가
