# ADR-001 재고 조회 API를 SKU 집계·로트 상세·Lot 마스터로 분리

## 상태
`Accepted` (2026-09-21)

## 컨텍스트
로트 기반 재고 구조에서 `GET /inventory` 하나로 SKU 집계 뷰와 로트별 상세 뷰를 동시에 처리하려 하면, 응답 스키마가 뷰에 따라 달라지고 쿼리 파라미터 분기(`view=sku`/`view=lot`)가 API 계약을 복잡하게 만든다. 또한 `Lot`(SKU+공급처+로트번호 마스터, 수량 없음)과 `InventoryLot`(구역별 수량 원장)이 서로 다른 개념인데 하나의 엔드포인트 이름(`/inventory`)에 섞이면 혼동이 크다.

## 결정
재고 조회를 3개 엔드포인트로 분리한다.
- `GET /inventory` — SKU 기준으로 모든 로트·구역을 합산한 집계 뷰. 대시보드·발주 판단용 기본 화면.
- `GET /inventory/by-lot` — `InventoryLot` 기준 로트·구역 단위 상세. FEFO 판단, 유통기한 관리용.
- `GET /lots`, `GET /lots/{lotId}` — `Lot` 마스터 자체 조회. 수량 정보 없음.

## 근거
- 각 엔드포인트의 응답 스키마가 고정되어 프론트엔드 소비가 단순해진다.
- SKU 집계와 로트 상세는 실제로 쓰이는 화면(재고 현황판 vs 유통기한 관리)이 다르므로 분리하는 편이 자연스럽다.
- `Lot`과 `InventoryLot`의 개념 차이가 엔드포인트 이름에도 드러나 유지보수 시 혼동을 줄인다.

## 검토했던 대안
- `GET /inventory?view=sku|lot` 하나로 통합 — 기각. 쿼리 파라미터에 따라 응답 스키마가 바뀌는 API는 클라이언트 타입 처리가 복잡해지고, 문서화도 어려워진다.

## 영향
- API 명세에 `/inventory/by-lot`, `/lots`, `/lots/{lotId}` 3개 엔드포인트 반영 완료.
- `Lot`은 별도 생성 API 없이 입고 검수 트랜잭션에서 find-or-create로 생성한다 ([ADR-004](004-lot-find-or-create-on-inspect.md) 참고).
