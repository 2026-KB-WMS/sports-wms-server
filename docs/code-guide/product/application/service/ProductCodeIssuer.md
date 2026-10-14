# ProductCodeIssuer

`product/application/service/ProductCodeIssuer.java` · `@Component`, **package-private**

상품 코드를 발급한다. 형식은 `브랜드코드-카테고리코드-일련번호4자리` (예: `YNX-RKT-0001`, ADR-026).

## 의존

`BrandRepository`, `ProductCodeSequenceRepository`, `ProductRepository`

## 메서드

| 메서드 | 설명 |
|---|---|
| `issue(brand, category)` | 코드를 발급해 반환. `@Transactional(propagation = MANDATORY)`라서 호출자의 트랜잭션 안에서만 실행된다 |

## issue 동작

1. 브랜드에 코드가 없으면 `BRAND_CODE_REQUIRED`.
2. 브랜드 행을 `findByIdForUpdate`로 잠가 같은 브랜드의 발급을 직렬화한다(조합의 첫 채번 행을 만드는 순간의 경합 방지).
3. 브랜드+카테고리 조합별 채번 행의 `next`로 번호를 올려 `prefix + %04d` 형태로 만든다.
4. 과거에 직접 입력된 코드와 겹치면(`existsByProductCode`) 다음 번호로 넘어간다.

사용처: `ProductService.registerProduct`, `ProductService.updateProduct`(브랜드·카테고리 변경 시 재발급).
