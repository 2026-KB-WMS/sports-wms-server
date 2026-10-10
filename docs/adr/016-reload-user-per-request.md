# ADR-016: 토큰은 사용자 ID만 담고, 역할·상태·소속은 요청마다 DB에서 읽는다

- 상태: Accepted
- 날짜: 2026-10-10

## 컨텍스트
ADR-011은 토큰 클레임에 `role`, `warehouseIds`, `storeIds`를 담고 만료(1시간)까지 신뢰했다. 그래서 담당 창고·지점 배정이 해제되거나 계정이 비활성화·강등된 뒤에도 최대 1시간 동안 이전 권한으로 호출할 수 있었다. 관리자 API(`/users/**`)도 같았다.

## 결정
- 토큰 클레임은 `sub`(userId)만 둔다. `role`, `warehouseIds`, `storeIds` 클레임은 만들지 않고, 들어 있어도 읽지 않는다.
- `JwtAuthenticationFilter`는 토큰 검증 뒤 `AuthenticatedUserResolver`로 사용자를 조회해 `AuthenticatedUser`를 만든다. 사용자가 없거나 ACTIVE가 아니면 인증하지 않아 401이다.
- 포트(`AuthenticatedUserResolver`)는 `common.security`에 두고, 구현(`AuthenticatedUserService`)은 auth 도메인이 맡는다. `common`이 auth 애플리케이션 계층에 의존하지 않는다.
- 본사 관리자는 소속이 없으므로 소속 조회를 건너뛴다. 그 외 역할은 요청마다 사용자 1건과 소속 조회(창고·지점 ID)를 읽는다.
- 서비스의 소속 검사(`requireWarehouseAccess/requireStoreAccess`)와 ADR-012의 검사 순서는 그대로다. 값의 출처만 토큰에서 DB로 바뀐다.
- 비밀번호 변경 시 기존 토큰을 무효화하지는 않는다. 즉시 로그아웃·토큰 폐기가 필요하면 리프레시 토큰 또는 토큰 버전이 필요하며 "[보류]"에 남긴다.

## 근거
- 소속·역할·계정 상태 변경이 다음 요청부터 반영된다. 비활성화된 계정이 만료 전까지 API를 호출하던 문제도 함께 사라진다.
- 인가 값이 한 곳(DB)이라 토큰과 DB가 어긋날 여지가 없다. 서비스 코드는 바꿀 필요가 없다.
- 요청당 PK 조회 1건과 소속 조회 2건(`user_warehouse`, `user_store` 계열)이 추가되지만 인덱스 조회라 부담이 작다고 판단했다.

## 검토했던 대안
- 토큰 버전(`tokenVersion`) — 쿼리는 줄지만 컬럼·마이그레이션이 필요하고, 변경 후 재로그인해야 새 소속이 반영된다. 기각.
- 사용자별 짧은 캐시 — 쿼리는 줄지만 반영이 캐시 시간만큼 늦고 다중 서버에서 일관성 문제가 생긴다. 기각(조회 부하가 문제가 되면 재검토).
- 현행 유지(토큰 값을 만료까지 신뢰) — 배정 해제·비활성화가 최대 1시간 늦게 반영되는 보안 공백이 남아 기각.

## 영향
- 모든 인증 요청에서 사용자 조회가 추가된다. 사용자 조회 쿼리 수나 응답 시간이 문제가 되면 캐시를 검토한다.
- `JwtProvider.createAccessToken(userId)`, `parseUserId(token)`으로 시그니처가 바뀐다. 이미 발급된 옛 토큰(역할·소속 클레임 포함)은 `sub`만 쓰이므로 그대로 동작한다.
- `/auth/me`는 INACTIVE 계정이 더 이상 조회하지 못한다(401).
- 필터가 `AuthenticatedUserResolver` 빈을 필요로 해서, `SecurityConfig`를 가져오는 `@WebMvcTest`는 이 포트를 목으로 등록한다.
