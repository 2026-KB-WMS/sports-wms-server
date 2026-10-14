# OptionGroup

`product/domain/entity/OptionGroup.java`

색상·사이즈처럼 SKU를 구성하는 옵션 그룹. 특정 상품에 종속되지 않는 공용 마스터다.

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| optionGroupId | Long | PK |
| name | String | 그룹명 (유니크 `uk_option_group_name`) |
| createdAt, updatedAt | LocalDateTime | 생성·수정 시각 |

## 메서드

| 메서드 | 설명 |
|---|---|
| `register(name)` | 옵션 그룹 생성 |

수정·상태 변경 메서드는 없다(생성 전용).
