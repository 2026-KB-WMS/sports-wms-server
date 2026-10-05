# ADR-010: 지점 발주 진행 단계(progressStage)는 저장하지 않고 파생 값으로 계산

- 상태: Accepted
- 날짜: 2026-10-04

## 컨텍스트
지점 발주(StoreOrder)의 화면 표시용 "진행 단계"(승인 대기, 출고 준비 중, 배송 중, 일부 납품 등)는 발주 상태(StoreOrderStatus)와 최신 출고 상태(출고 도메인), 라인별 출고 부족 여부의 조합으로 결정된다. 이 값을 DB에 저장할지, 조회 시 계산할지 정해야 했다.

## 결정
`progressStage`는 DB에 저장하지 않는 읽기 전용 파생 값으로 두고, `StoreOrderProgressStage.resolve(status, latestOutboundStatus, hasShortage)`로 조회 시점에 계산한다.

## 근거
- 출고 상태는 출고 도메인이 소유한다. 발주 쪽에 사본을 저장하면 두 도메인 상태를 항상 동기화해야 하고, 어긋날 위험이 생긴다.
- 발주 상태 전이 규칙(StoreOrder 도메인)은 출고 진행과 독립적으로 단순하게 유지된다.
- 계산 규칙이 한 곳(enum의 정적 메서드)에 있어 단위 테스트로 전부 검증할 수 있다.

## 검토했던 대안
- 발주 상태에 출고 관련 값(PREPARING, IN_TRANSIT 등)을 추가로 저장하고 출고 이벤트마다 동기화: 상태 불일치 위험과 도메인 간 결합이 커서 제외.
- 클라이언트에서 계산: 규칙이 클라이언트마다 중복·불일치할 수 있어 제외.

## 영향
- 목록/상세 조회 시 출고 최신 상태를 `StoreOrderOutboundPort`로 조회해야 한다. 출고 도메인 구현 전에는 임시 어댑터가 빈 값을 반환해 ASSIGNED 발주가 PREPARING으로만 표시됐고, 구현(#143) 뒤에는 실제 출고 상태로 PREPARING·IN_TRANSIT·PARTIALLY_DELIVERED가 계산된다.
- 진행 단계로 검색하는 조건은 제공하지 않는다(상태 `status`로만 필터).
