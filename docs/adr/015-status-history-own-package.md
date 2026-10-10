# ADR-015 상태 이력은 `common`이 아닌 독립 도메인 패키지(`statushistory`)로 둔다

## 상태
`Accepted` (2026-10-10)

## 컨텍스트
상태 이력(`StatusHistory`)은 `com.kb.wms.common.statushistory`에 있었다. `common`은 SecurityConfig, GlobalExceptionHandler, ApiResponse 같은 공통 코드를 두는 곳인데, 상태 이력은 도메인 모델·포트·어댑터·마이그레이션을 가진 업무 모듈이고 `StatusHistoryEntityType`으로 USER·INBOUND·OUTBOUND 등 업무 도메인을 알고 있다. 또 처리자 이름을 채우려면 `common`이 auth 엔티티를 참조해야 해서 ADR-007에서 조인을 보류했다.

## 결정
- 패키지를 `com.kb.wms.common.statushistory`에서 `com.kb.wms.statushistory`로 옮긴다. 내부 구조(헥사고날 ports/adapters)와 클래스 이름은 그대로 둔다.
- 테이블은 기존처럼 `status_history` 1개를 유지한다. `entity_type` + `entity_id` 다형 참조 구조도 바꾸지 않는다.
- 이번 변경은 패키지 이동만이다. DB 스키마, API 응답, 동작은 바뀌지 않는다.

## 근거
- 7개 엔티티 유형의 이력 컬럼(`from_status`, `to_status`, `reason`, `changed_by`, `changed_at`)이 모두 같다. 도메인별 테이블로 나누면 같은 모양의 엔티티·포트·어댑터·마이그레이션이 7벌 생기고, 이력 로직 수정이 7곳에 퍼지며, 사용자 기준 횡단 조회가 UNION이 된다.
- 어색함의 원인은 테이블이 아니라 "`common`이 업무 도메인을 안다"는 위치 문제였다. 독립 도메인으로 두면 다른 도메인과 같은 규칙(도메인 패키지 + ADR-007의 읽기 전용 조인)을 적용할 수 있다.

## 검토했던 대안
- 도메인별 이력 테이블(`inbound_history` 등) — 기각. 위 근거의 중복·횡단 조회 비용이 크고, 지금은 도메인별로 다른 이력 필드나 FK 무결성 요구가 없다. 그런 요구가 생기면 다시 검토한다.
- `common`에 그대로 유지 — 기각. 위치가 의미와 맞지 않고 auth 참조 문제가 계속 남는다.

## 영향
- `src/main`, `src/test`의 `common/statushistory` 디렉터리와 import 약 39개 파일이 바뀐다.
- `StatusHistory.changedByName`(예: 지점 발주 상세의 `statusHistory[].changedByName`)은 여전히 `null`이다. 패키지가 분리되었으므로 `users` 조인으로 채우는 일은 후속 작업이다(ADR-007).
- `AGENTS.md`의 도메인 목록에 `statushistory`를 추가한다.
