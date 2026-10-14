# OptionValue

`product/domain/entity/OptionValue.java`

옵션 그룹에 속하는 구체적 옵션 값(예: 빨강, L).

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| optionValueId | Long | PK |
| optionGroupId | Long | 소속 옵션 그룹 ID |
| optionCode | String | SKU 코드 자동 생성에 쓰는 영문 대문자·숫자 코드. 코드 도입 전 값은 null (ADR-025) |
| value | String | 표시 값 (그룹 내 유니크 `uk_option_value_group_value`) |
| sortOrder | int | 표시 순서 |
| status | ProductStatus | 생성 시 null이면 ACTIVE |
| createdAt, updatedAt | LocalDateTime | 생성·수정 시각 |

## 메서드

| 메서드 | 설명 |
|---|---|
| `register(optionGroupId, optionCode, value, sortOrder)` | ACTIVE 상태로 생성 |
| `activate()` / `deactivate()` / `isActive()` | 상태 변경·조회. 비활성 값은 SKU에 연결 불가(`OPTION_VALUE_INACTIVE`) |
