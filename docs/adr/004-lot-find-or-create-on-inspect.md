# ADR-004 Lot은 별도 생성 API 없이 입고 검수 트랜잭션 내 find-or-create로 생성

## 상태
`Accepted` (2026-09-21)

## 컨텍스트
`Lot`(SKU+공급처+로트번호 마스터)을 언제, 어떤 API로 생성할지가 불명확했다. 별도의 `POST /lots` 생성 API를 두는 방안과, 입고 검수 시점에 자동으로 생성하는 방안을 검토했다.

## 결정
`Lot`은 독립된 생성 API를 두지 않는다. `PATCH /inbounds/{id}/inspect` 트랜잭션 내부에서 SKU+공급처+로트번호 조합이 이미 있으면 재사용하고, 없으면 새로 생성하는 find-or-create 방식으로 처리한다. 조회 전용 `GET /lots`, `GET /lots/{lotId}`만 제공한다.

## 근거
- `Lot`은 실제로 입고 검수 시점에만 새로 생겨나는 개념이라, 별도 생성 화면·API를 두면 입고 프로세스와 별개로 데이터 정합성이 깨질 위험이 있다(예: 발주와 무관한 로트가 먼저 생성됨).
- `Lot`은 검수(`inspect`) 트랜잭션에서 find-or-create로 생성하고, 재고 행(`InventoryLot`)은 완료(`complete`) 트랜잭션에서 반영한다. `InboundLine.lot_id`가 NOT NULL이라 검수 항목을 저장하려면 로트가 먼저 있어야 하기 때문이다. 검수 중 취소된 입고에는 재고 없이 `Lot`만 남을 수 있으며, 로트는 마스터라 남아도 무해하다.

## 검토했던 대안
- `POST /lots` 별도 생성 API 제공 — 기각. 입고 흐름과 분리된 생성 경로가 생기면 검수 없이 로트가 먼저 만들어지는 등 순서 오류 가능성이 커진다.

## 영향
- API 명세: `POST /lots` 없음. `GET /lots`, `GET /lots/{lotId}` 조회 전용 엔드포인트만 존재.
- ERD 데이터 사전의 `Lot`/`InventoryLot` 스키마는 이미 이 결정과 일치함을 확인함(수정 불필요).
