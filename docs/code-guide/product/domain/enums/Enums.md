# product 도메인 enum

`product/domain/enums/`

## OptionType

카테고리에 지정하는 옵션 그룹의 용도(ADR-025).

| 값 | 의미 |
|---|---|
| SPEC | 상품 하나에 값이 정해지는 스펙 |
| SKU | SKU마다 달라지는 옵션 |

## ProductStatus

상품 도메인 공통 사용 여부 상태. Brand, Category, Product, ProductSku, OptionValue가 함께 쓴다.

| 값 | 의미 |
|---|---|
| ACTIVE | 사용 중 |
| INACTIVE | 비활성 |

| 메서드 | 설명 |
|---|---|
| `fromActiveFlag(Boolean isActive)` | 목록 조회의 `isActive` 필터를 상태로 변환. null이면 null(조건 없음), true면 ACTIVE, false면 INACTIVE |

## SkuUnit

SKU 수량 단위(`ProductSku.unit`)의 서버 허용 목록. 등록 요청 검증과 `GET /api/v1/products/skus/units` 응답에 쓴다(ADR-024).

| 값 | 설명(description) |
|---|---|
| EA | 개 |
| BOX | 박스 |
| SET | 세트 |

| 메서드 | 설명 |
|---|---|
| `getCode()` | enum 이름 그대로 반환 |
| `getDescription()` | 한글 표시명 |
| `isValidCode(code)` | 허용 목록에 있는 코드인지 확인 |
