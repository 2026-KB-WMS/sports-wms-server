# ADR-012: 인가 검사 위치 — 역할은 보안 설정, 소속·작성자는 서비스

- 상태: Accepted
- 날짜: 2026-10-08

## 컨텍스트
인증 도메인(#162)에서 기존 도메인 전체에 인증·인가를 적용하기 전에(#170), 현재 사용자 컨텍스트를 어떻게 다루고 어떤 검사를 어디서 할지 정해야 했다. 지금은 컨트롤러가 처리 사용자를 `userId` 쿼리 파라미터로 받는다.

## 결정
- 현재 사용자 컨텍스트는 `common.security.AuthenticatedUser`(userId, role, warehouseIds, storeIds)다. 별도 `AuthContext` 타입을 새로 만들지 않는다. 컨트롤러가 `@AuthenticationPrincipal`로 받아 처리 사용자 ID로 쓰고, 소속·작성자 검사가 필요한 유스케이스에는 인자로 넘긴다.
- **역할 검사**(이 API를 부를 수 있는 역할인가)는 `SecurityConfig`의 경로·메서드 규칙에 모은다. 표는 [`api/authorization.md`](../api/authorization.md)와 1:1로 맞춘다.
- **소속 범위 검사**(담당 창고·지점의 리소스인가)와 **작성자 검사**는 서비스에서 한다. 대상 리소스의 창고·지점은 읽어 봐야 알 수 있기 때문이다. 소속은 `AuthenticatedUser.requireWarehouseAccess/requireStoreAccess`로 검사하고, 위반은 403 `FORBIDDEN`(`BusinessException`)이다. HQ_ADMIN은 항상 통과한다.
- 목록 조회의 소속 범위 제한은 서비스가 `warehouseId`·`storeId` 조건을 담당 ID로 좁히는 방식으로 한다.

## 근거
- 토큰에 소속 ID가 있어 검사에 DB 조회가 필요 없다([ADR-011](011-jwt-access-token-only.md)).
- 역할 규칙을 한 파일에 두면 역할 매트릭스 문서와 대조하기 쉽고, 컨트롤러마다 흩어진 애노테이션을 찾을 필요가 없다.
- 소속 검사를 컨트롤러에서 하면 서비스가 읽은 리소스를 컨트롤러가 다시 읽어야 한다.

## 검토했던 대안
- `@PreAuthorize`로 역할·소속을 컨트롤러/서비스 메서드에 선언: 소속 검사는 리소스의 창고 ID를 SpEL에서 알아야 해서 복잡해지고, 규칙이 여러 파일에 흩어진다.
- `AuthContext` 인터페이스로 `AuthenticatedUser`를 감싸기: 구현체가 하나뿐이라 추상화 이득이 없다.

## 영향
- 서비스 유스케이스가 `common.security.AuthenticatedUser`를 인자로 받을 수 있다(`common`은 공용 영역이라 의존 방향 문제가 없다).
- 소속 검사는 토큰 값 기준이라, 소속 변경은 토큰 만료 전까지 반영되지 않는다.
- 도메인별 적용은 #170에서 PR을 나눠 진행하고, 마지막 PR에서 `SecurityConfig`를 "로그인·가입 외 전체 인증 필요"로 바꾼다.
