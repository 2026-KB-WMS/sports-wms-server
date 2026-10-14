# 아웃바운드 포트

`auth/application/port/out/`

## UserRepository

| 메서드 | 설명 |
|---|---|
| `save(user)` | 저장. **`saveAndFlush`**로 `@LastModifiedDate`가 채워져 응답의 `updatedAt`이 이번 수정 시각이 된다 |
| `findById(id)` | 단건 |
| `findByIdForUpdate(id)` | 행을 잠그고 조회. 상태 변경처럼 동시 수정을 막아야 할 때 |
| `findByLoginId(loginId)` | 아이디로 조회. 아이디는 **소문자로 통일해 저장**하므로 호출 전에 같은 형식으로 맞춘다 |
| `search(condition)` | 생성 일시 내림차순 |
| `existsByLoginId`, `existsByEmail` | 중복 확인 |
| `existsByEmailAndUserIdNot(email, userId)` | 본인을 제외한 이메일 중복 (수정 시) |
| `existsByRole(role)` | 해당 역할의 계정이 하나라도 있는지 (최초 본사 관리자 생성 판단) |

## UserQueryRepository

읽기 전용 조회 포트. 다른 도메인 테이블(창고·지점 소속)은 **ID 기준 읽기 전용 조인**으로만 읽는다(ADR-007).

| 메서드 | 설명 |
|---|---|
| `findAffiliation(userId)` | 사용자에게 배정된 창고·지점 ID. 소속이 없으면 빈 목록 |
| `findNamesByIds(userIds)` | 사용자 ID별 이름 맵. 존재하지 않는 ID는 결과에 없다. 입력이 비어 있으면 빈 맵 |
