# ADR-012: 인가 검사 위치 — 역할은 보안 설정, 소속·작성자는 서비스

- 상태: Accepted
- 날짜: 2026-10-08

## 컨텍스트
인증 도메인(#162)에서 기존 도메인 전체에 인증·인가를 적용하기 전에(#170), 현재 사용자 컨텍스트를 어떻게 다루고 어떤 검사를 어디서 할지 정해야 했다. 당시에는 컨트롤러가 처리 사용자를 `userId` 쿼리 파라미터로 받았다.

## 결정
- 현재 사용자 컨텍스트는 `common.security.AuthenticatedUser`(userId, role, warehouseIds, storeIds)다. 별도 `AuthContext` 타입을 새로 만들지 않는다. 컨트롤러가 `@AuthenticationPrincipal`로 받아 처리 사용자 ID로 쓰고, 소속·작성자 검사가 필요한 유스케이스에는 인자로 넘긴다.
- **역할 검사**(이 API를 부를 수 있는 역할인가)는 `SecurityConfig`의 경로·메서드 규칙에 모은다. 표는 [`api/authorization.md`](../api/authorization.md)와 1:1로 맞춘다.
- **소속 범위 검사**(담당 창고·지점의 리소스인가)와 **작성자 검사**는 서비스에서 한다. 대상 리소스의 창고·지점은 읽어 봐야 알 수 있기 때문이다. 소속은 `AuthenticatedUser.requireWarehouseAccess/requireStoreAccess`로 검사하고, 위반은 403 `FORBIDDEN`(`BusinessException`)이다. HQ_ADMIN은 항상 통과한다.
- 사용자 요청용 유스케이스 메서드는 `AuthenticatedUser actor`를 인자로 받고 서비스가 검사한다. 다른 도메인이 내부 조회에 함께 쓰는 메서드(예: `WarehouseUseCase.getWarehouse`, `WarehouseSectionUseCase.getSection`)는 주체 없이 호출되어야 하므로 기존 시그니처를 그대로 두고 `actor`를 받는 오버로드를 따로 둔다. 재고 행의 창고처럼 쓰기에 필요한 소속도 서비스가 행을 잠근 뒤 같은 트랜잭션에서 검사한다(재고 조정).
- 검사 순서: 소속 범위(403)를 상태(409)보다 먼저 확인한다. 락을 잡는 변경은 락 직후, 상태 검사 이전에 소속을 확인하고, 작성자·상태별 세부 권한은 상태 검사 뒤에 둔다(상세는 `docs/api/authorization.md`).
- 목록 조회의 소속 범위 제한은 서비스가 `AuthenticatedUser.warehouseScope`로 담당 ID 목록을 만들어 검색 조건(`warehouseIds`)에 담고, 영속성 쿼리가 `in` 조건으로 좁히는 방식으로 한다. 담당 창고가 없으면 빈 결과다. 구역처럼 소속을 간접 참조하는 조건은 서비스가 조회 포트로 창고를 알아낸 뒤 검사한다.

## 근거
- 토큰에 소속 ID가 있어 검사에 DB 조회가 필요 없다([ADR-011](011-jwt-access-token-only.md)).
- 역할 규칙을 한 파일에 두면 역할 매트릭스 문서와 대조하기 쉽고, 컨트롤러마다 흩어진 애노테이션을 찾을 필요가 없다.
- 소속 검사는 업무 규칙이므로 웹 계층이 아니라 서비스에 두어야 다른 진입점(배치, 다른 유스케이스)에서도 우회되지 않고, 서비스 단위 테스트로 검증할 수 있다.

## 검토했던 대안
- `@PreAuthorize`로 역할·소속을 컨트롤러/서비스 메서드에 선언: 소속 검사는 리소스의 창고 ID를 SpEL에서 알아야 해서 복잡해지고, 규칙이 여러 파일에 흩어진다.
- `AuthContext` 인터페이스로 `AuthenticatedUser`를 감싸기: 구현체가 하나뿐이라 추상화 이득이 없다.

## 영향
- 서비스 유스케이스가 `common.security.AuthenticatedUser`를 인자로 받을 수 있다(`common`은 공용 영역이라 의존 방향 문제가 없다).
- 소속 검사는 토큰 값 기준이라, 소속 변경은 토큰 만료 전까지 반영되지 않는다.
- 도메인별 적용은 #170에서 진행했고, 마지막 도메인(출고)에서 `SecurityConfig`를 "가입·로그인·API 문서·헬스 체크 외 전체 인증 필요"로 바꿨다.
