# BrandService

`product/application/service/BrandService.java` · 구현: `BrandQueryUseCase` · 클래스 기본 `@Transactional(readOnly = true)`

브랜드 등록·수정·조회.

## 의존

`BrandRepository`

## 메서드

| 메서드 | 트랜잭션 | 설명 |
|---|---|---|
| `registerBrand(command)` | 쓰기 | 이름 중복 → `DUPLICATE_BRAND_NAME`. 코드는 영문 3자 검증 후 대문자로 정규화, 중복 → `DUPLICATE_BRAND_CODE`. 저장 |
| `updateBrand(command)` | 쓰기 | 변경 필드가 없으면 `VALIDATION_ERROR`. 브랜드 없음 → `BRAND_NOT_FOUND`. 아래 규칙 참고 |
| `getBrands(condition)` | 읽기 | 검색 조건으로 목록 조회 |
| `getBrand(brandId)` | 읽기 | 단건 조회. 없으면 `BRAND_NOT_FOUND` |
| `normalizeBrandCode(code)` (private) | - | `^[A-Za-z]{3}$` 검사 후 `toUpperCase`. 웹 검증을 안 거친 호출도 같은 규칙을 지키게 한다 |

## updateBrand 규칙

- `brandCode`가 넘어오면: 이미 코드가 있는 브랜드는 `VALIDATION_ERROR`(한 번 지정하면 변경 불가), 없으면 자기 자신을 제외한 중복 검사 후 `assignBrandCode`
- `name`이 넘어오면: 자기 자신 제외 중복 검사(`DUPLICATE_BRAND_NAME`) 후 변경
- `description`이 넘어오면 변경
- null인 필드는 건드리지 않는 부분 수정
