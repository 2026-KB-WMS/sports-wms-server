# ADR-011: 액세스 토큰만 발급하는 JWT 인증 (HS256, jjwt)

- 상태: Accepted (개정: 클레임과 소속·역할·상태 반영 방식은 [ADR-016](016-reload-user-per-request.md)이 대체)
- 날짜: 2026-10-06

## 컨텍스트
인증 도메인(#162) 구현에서 로그인 후 API 호출 방식을 정해야 했다. 토큰 종류·만료, 클레임, JWT 라이브러리, 인증 강제 전환 시점이 열려 있었다.

## 결정
- 액세스 토큰(JWT, HS256)만 발급하고 만료는 1시간(`wms.jwt.access-token-validity-seconds`)이다. 리프레시 토큰과 로그아웃은 만들지 않는다.
- ~~클레임은 `sub`(userId), `role`, `warehouseIds`, `storeIds`다. 비활성화·소속 변경 뒤에도 클레임을 신뢰하고 만료까지 유지한다.~~ → ADR-016: 클레임은 `sub`(userId)만 두고, 역할·상태·소속은 요청마다 DB에서 읽어 즉시 반영한다.
- 서명 키는 `wms.jwt.secret`(32바이트 이상)이며 prod 프로파일에서는 `JWT_SECRET` 환경변수로만 받는다. 키가 짧으면 기동이 실패한다. 기본 프로파일의 키는 로컬 개발용이다.
- 라이브러리는 jjwt 0.12.6을 쓰고 JSON 처리는 `jjwt-gson`으로 한다. Spring Boot 4는 Jackson 3(`tools.jackson`)을 쓰는데 `jjwt-jackson`은 Jackson 2에 의존하기 때문이다.
- `JwtAuthenticationFilter`는 유효한 Bearer 토큰이면 `AuthenticatedUser`를 SecurityContext에 올리고, 없거나 유효하지 않으면 그대로 넘긴다. 거절은 `authorizeHttpRequests` 규칙이 한다. 인증·인가 실패 응답(401/403)은 `RestSecurityExceptionHandler`가 공통 오류 포맷으로 쓴다.
- 기존 도메인 인증 적용(#170) 전까지 `SecurityConfig`는 기존 엔드포인트를 `permitAll`로 둔다. 필터는 토큰을 파싱만 한다. 예외로 인증 도메인 API만 #167부터 규칙을 적용한다: `/api/v1/auth/me`는 인증 필수, `/api/v1/users/**`는 `HQ_ADMIN`만 허용한다.
- JWT 관련 클래스는 `common.security`에 두며, 역할 검사에 `auth.domain.enums.UserRole`을 직접 쓴다.

## 근거
- 웹 클라이언트가 하나(본사·창고·지점 화면)이고 업무 시간 단위 세션이라 리프레시 토큰의 복잡도(저장·회전·폐기)에 비해 이득이 작다.
- 소속 ID를 클레임에 넣으면 요청마다 소속 조회 쿼리 없이 범위 검사를 할 수 있다.
- 필터가 파싱만 하게 해서 인증 강제를 마지막 PR로 미루고, 그 전까지 기존 API와 테스트를 깨지 않는다.

## 검토했던 대안
- 액세스 + 리프레시 토큰: 만료 연장과 즉시 무효화가 가능하지만 저장소·회전 정책이 필요해 이번 범위에서 제외(보류).
- 서버 세션: 기존 설계가 stateless(`SessionCreationPolicy.STATELESS`)라 제외.
- Spring Security OAuth2 Resource Server(Nimbus): 표준이지만 자체 발급 로그인에는 설정이 과해 제외.
- `jjwt-jackson`: Jackson 2 의존이라 제외.

## 영향
- 계정을 비활성화하거나 소속·역할을 바꿔도 이미 발급된 토큰은 최대 1시간 유효하다. 관리자 API(`/users/**`)도 같다: 강등·비활성화된 관리자가 만료 전까지 호출할 수 있고, `/auth/me`는 INACTIVE로 바뀐 계정도 조회할 수 있다. 즉시 무효화가 필요해지면 리프레시 토큰 또는 토큰 버전 관리가 필요하며 "[보류]"에 기록한다.
- 새 환경(prod)에서는 `JWT_SECRET`을 반드시 설정해야 한다. docker-compose는 기본 프로파일이라 개발용 키로 동작한다.
- `common.security`가 `auth.domain.enums.UserRole`을 참조한다.
