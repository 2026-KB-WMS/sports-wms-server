# StoreMemberService

`store/application/service/StoreMemberService.java` · 구현: `StoreMemberUseCase` · 클래스 기본 `@Transactional(readOnly = true)`

지점 관리자(점주) 배정·조회·배정 해제.

## 의존

`StoreMemberRepository`, `StoreRepository`, `StoreMemberUserPort`(auth 도메인 연결은 `StoreMemberUserAdapter`)

## 메서드

| 메서드 | 트랜잭션 | 설명 |
|---|---|---|
| `assignManager(command)` | 쓰기 | 아래 검증 순서 |
| `getManagers(storeId, userId, keyword)` | 읽기 | `storeId`가 있으면 존재 확인(`STORE_NOT_FOUND`) 후 검색 |
| `releaseManager(memberId)` | 쓰기 | 없으면 `MEMBER_NOT_FOUND`, 있으면 삭제 |

## assignManager 검증 순서

| 순서 | 검사 | 오류 |
|---|---|---|
| 1 | `memberRole`이 허용 값(OWNER, MANAGER)인지 | 400 `VALIDATION_ERROR` |
| 2 | 지점 존재 | 404 `STORE_NOT_FOUND` |
| 3 | 지점 활성 | 409 `CONFLICT` |
| 4 | 사용자 존재 | 404 `USER_NOT_FOUND` |
| 5 | 사용자 역할이 `STORE_OWNER` | 400 `VALIDATION_ERROR` |
| 6 | 사용자 상태가 `ACTIVE` | 409 `CONFLICT` |
| 7 | 같은 (지점, 사용자) 배정이 없음 | 409 `ALREADY_ASSIGNED` |

승인 전 사용자의 소속은 가입 승인 요청(`PATCH /users/{userId}`)에서 함께 지정한다. 결과는 확인한 사용자의 이름·로그인 아이디로 채운 `StoreMemberView`다.
