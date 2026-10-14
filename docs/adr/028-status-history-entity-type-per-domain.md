# ADR-028: 상태 이력의 엔티티 종류는 소유 도메인이 정의하고, 이력 모듈은 문자열로 받는다

- 상태: Accepted
- 날짜: 2026-10-14

## 컨텍스트
상태 이력은 `statushistory`가 `StatusHistoryEntityType` enum(USER, INBOUND, STORE_ORDER 등)을 정의하고, 모든 도메인이 그 enum을 import해 `record(...)`를 호출했다(ArchUnit 점검에서 타 도메인 enum 의존 13건). 이력은 엔티티를 소유한 도메인이 남기는 것인데, 이력 모듈이 모든 도메인의 엔티티 종류를 알고 있어 도메인 간 경계가 뚫려 있었다. 지점 발주를 OMS로 분리하면 발주 이력(`STORE_ORDER`)이 함께 나가야 하는데, 공유 enum이 있으면 두 서비스가 같은 타입에 묶인다.

## 결정
- **이력 모듈은 엔티티 종류를 문자열로 받는다.** `StatusHistoryUseCase`, `StatusHistory`, 저장소, JPA 엔티티의 `entityType`을 `String`으로 한다. 이력 모듈은 값의 의미를 모르고 "종류·엔티티 ID·상태 전이·사유·처리자"만 저장한다.
- **엔티티 종류는 소유 도메인이 자기 enum으로 정의한다.** `AuthHistoryEntityType`(USER), `InboundHistoryEntityType`(PURCHASE_ORDER, INBOUND), `InventoryHistoryEntityType`(LOT), `OutboundHistoryEntityType`(OUTBOUND, STOCK_ALLOCATION), `StoreHistoryEntityType`(STORE), `StoreOrderHistoryEntityType`(STORE_ORDER). 호출 쪽은 `name()`을 넘긴다.
- **저장 값은 바꾸지 않는다.** 컬럼은 그대로 `status_history.entity_type VARCHAR(30)`이고 값도 기존 enum 이름과 같다. 마이그레이션과 기존 데이터 변환은 없다. 이미 쓰는 값의 이름을 바꾸면 저장된 이력과 어긋나므로 바꾸지 않는다.
- **종류 값은 도메인 사이에서 겹치지 않아야 한다.** 이력 모듈이 값을 검사하지 않으므로 새 값을 추가할 때 다른 도메인의 값과 같은 이름을 쓰지 않는다.

## 근거
- 이력 모듈이 다른 도메인을 모르게 되어 `statushistory → 도메인`, `도메인 → statushistory enum` 의존이 사라진다.
- 발주를 분리할 때 이력 행(`entity_type = STORE_ORDER`)을 발주와 함께 옮기면 되고, 두 서비스가 이력 타입을 공유하지 않는다.
- 기존 값과 컬럼을 그대로 두어 배포·데이터 위험이 없다.

## 검토했던 대안
- 공유 enum 유지 + 공용 위치(`common`)로 이동: 기각. 의존 방향만 바뀌고 모든 도메인이 한 타입에 묶이는 문제는 그대로다.
- 도메인별 enum 없이 문자열 상수를 호출부에 직접 작성: 기각. 오타와 중복을 컴파일러가 잡지 못한다.
- 이력 모듈에서 허용 값 목록을 검사: 보류. 값을 알아야 하므로 이번 결정의 목적(이력 모듈이 도메인을 모름)과 어긋난다.

## 영향
- 코드: `StatusHistoryEntityType` 삭제, 도메인별 enum 6개 추가, 이력 모듈과 호출 서비스·어댑터, 관련 테스트 수정.
- API 응답·DB 값 변화 없음. ADR-015의 "`StatusHistoryEntityType`으로 업무 도메인을 알고 있다"는 서술은 이 ADR로 대체된다.
- 문서: `docs/code-guide/statushistory/domain/StatusHistory.md`.
