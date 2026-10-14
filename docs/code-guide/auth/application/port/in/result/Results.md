# 결과 (Result)

`auth/application/port/in/result/`

## LoginResult

| 항목 | 내용 |
|---|---|
| 용도 | 로그인 성공 결과 |
| 만드는 곳 | `AuthService.login` |
| 쓰는 곳 | `LoginResponse.of` |
| 필드 | accessToken, expiresIn(토큰 유효 시간, 초), user(`User`) |

## UserAffiliation

| 항목 | 내용 |
|---|---|
| 용도 | 사용자의 창고·지점 소속 ID. 한 사용자가 여러 창고·지점에 배정될 수 있다 |
| 만드는 곳 | `UserQueryRepository.findAffiliation` (창고·지점 소속 테이블을 ID로 읽기 전용 조회) |
| 쓰는 곳 | `AuthenticatedUserService`(요청마다 인증 주체 생성), `UserService`(승인 시 소속 여부 확인) |
| 필드 | warehouseIds, storeIds (둘 다 오름차순, 불변 복사) |
| 메서드 | `hasAny()`: 창고 또는 지점 소속이 하나라도 있는지 |

## InitialHqAdminResult

| 값 | 의미 |
|---|---|
| CREATED | 본사 관리자가 없어서 설정값으로 새로 만들었다 |
| ALREADY_EXISTS | 이미 있어서 아무것도 하지 않았다 |
| NOT_CONFIGURED | 없지만 생성 설정이 비어 있어 만들지 못했다 |

`InitialHqAdminRunner`가 결과에 따라 로그를 남긴다.
