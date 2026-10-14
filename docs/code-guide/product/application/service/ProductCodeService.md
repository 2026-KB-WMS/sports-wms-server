# ProductCodeService

`product/application/service/ProductCodeService.java` · 구현: `ProductCodeUseCase` · `@Transactional(readOnly = true)`

상품 도메인의 고정 코드 목록 조회. DB를 조회하지 않고 확정된 enum 값을 그대로 반환한다.

## 메서드

| 메서드 | 설명 |
|---|---|
| `getSkuUnits()` | `SkuUnit` 전체를 `CodeItem(code, description)` 목록으로 반환 (EA/개, BOX/박스, SET/세트) |
