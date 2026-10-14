# OptionValueService

`product/application/service/OptionValueService.java` · 구현: `OptionValueUseCase` · 클래스 기본 `@Transactional(readOnly = true)`

옵션 값 등록.

## 의존

`OptionValueRepository`, `OptionGroupRepository`

## 메서드

| 메서드 | 트랜잭션 | 설명 |
|---|---|---|
| `registerOptionValue(command)` | 쓰기 | 옵션 그룹 없음 → `OPTION_GROUP_NOT_FOUND`. 같은 그룹에 같은 값 → `DUPLICATE_OPTION_VALUE`. 옵션 코드를 대문자로 바꿔 같은 그룹 내 중복 → `DUPLICATE_OPTION_CODE`. 저장 |

옵션 값 조회는 `OptionGroupService`, `CategoryOptionGroupService`가 그룹과 함께 처리한다.
