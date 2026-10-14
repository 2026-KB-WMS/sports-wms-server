# ProductSpec

`product/domain/entity/ProductSpec.java`

상품에 지정한 스펙 값. 상품당 옵션 그룹 하나에 값 하나만 둘 수 있다.

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| productSpecId | Long | PK |
| productId | Long | 상품 ID |
| optionValueId | Long | 지정한 옵션 값 ID |
| createdAt | LocalDateTime | 생성 시각 |

## 메서드

| 메서드 | 설명 |
|---|---|
| `assign(productId, optionValueId)` | 상품에 스펙 값을 지정하는 매핑 생성 |

"그룹당 값 하나" 제약은 이 클래스가 아니라 서비스(`OptionSelectionResolver`)에서 검사한다.
