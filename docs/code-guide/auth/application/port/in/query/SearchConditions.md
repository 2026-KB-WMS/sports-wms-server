# 검색 조건 (SearchCondition)

`auth/application/port/in/query/`

## UserSearchCondition

| 항목 | 내용 |
|---|---|
| 용도 | 사용자 목록 필터. **가입 승인 대기 목록은 `status=PENDING`**으로 조회한다 |
| 엔드포인트 | GET `/api/v1/users` (HQ_ADMIN 전용) |
| 만드는 곳 | `UserController.getUsers` |
| 쓰는 곳 | `UserService.getUsers` → `UserRepository.search` |
| 정렬 | 생성 일시 내림차순, 사용자 ID 내림차순 |

| 필드 | 설명 |
|---|---|
| role | 역할 필터 |
| status | 계정 상태 필터 |
| keyword | 이름·로그인 아이디·이메일 부분 일치(대소문자 무시) |

null인 조건은 무시한다.
