# ADR-007 재고 조회 API는 도메인 간 테이블을 JPQL로 직접 조인

## 상태
`Accepted` (2026-09-24)

## 컨텍스트
[ADR-005](005-entity-reference-by-id.md)는 "실제 데이터가 필요하면 해당 도메인의 레포지토리/서비스를 통해 조회한다"고 정했다. 그런데 재고 조회 API(SKU별 재고 집계, 로트 상세, 재고 이력 등)는 inventory_lot, lot, product_sku, warehouse_section, warehouse 등 최대 4개 도메인 테이블의 값을 한 화면에 함께 보여줘야 한다. ADR-005 원칙대로 서비스 계층에서 N+1 호출로 조합하면 목록 조회마다 반복 호출이 발생하고, 페이지네이션이 없는 현재 구조(별도 결정)에서는 성능 문제가 더 커진다.

## 결정
조회 전용(GET) API에 한해, 리포지토리 계층(JPQL)에서 도메인 테이블을 ID로 직접 JOIN하고, JPQL 생성자 표현식(`select new ...Result(...)`)으로 곧장 애플리케이션 결과 레코드(InventorySkuSummary, InventoryLotView, InventoryDetail, LowStockItem 등)를 만든다. 명령(등록/수정 등) 경로는 ADR-005 그대로 ID 참조 + 리포지토리/서비스 호출만 사용하고, 조회 경로에서만 예외를 둔다.

## 근거
- 조회는 여러 도메인 데이터를 한 응답에 모아 보여주는 것 자체가 목적이라, DB 레벨 JOIN이 애플리케이션 레벨 N+1 호출보다 훨씬 효율적이다.
- JPQL 생성자 표현식으로 바로 결과 레코드를 만들면 JPA 엔티티를 거치지 않아 불필요한 지연 로딩·매핑 코드가 없다(사실상 조회 전용 CQRS 스타일 분리).
- 명령(쓰기) 경로는 여전히 도메인 경계를 지키므로, ADR-005의 핵심 목적(패키지 경계 보호, 도메인 분리 가능성)은 훼손되지 않는다.

## 검토했던 대안
- 서비스 계층에서 다른 도메인 UseCase를 호출해 조합(ADR-005 원칙 그대로 적용) — 기각. 목록 조회 시 항목 수만큼 반복 호출이 발생해 N+1 문제가 생기고, 별도 캐싱/배치 조회 로직을 추가로 만들어야 해 복잡도가 커진다.
- 조회 전용 별도 읽기 모델(별도 뷰 테이블/역정규화 테이블) — 기각. 현재 규모에서는 과설계이며, 동기화 로직을 추가로 관리해야 한다.

## 개정 (2026-10-06, #168)
사용자 이름(`receivedByName`, `createdByName`, `allocatedByName`, 담당자 목록의 `userName`·`loginId`)도 같은 방식으로 채운다. 재고 조회뿐 아니라 입고·창고 발주·지점 발주·출고 할당·담당자 목록의 조회 전용 JPQL이 `users` 테이블을 ID로 조인한다(사용자가 없는 행이 사라지지 않도록 이름만 필요한 곳은 `left join`). 명령(쓰기) 경로의 사용자 확인은 계속 `UserUseCase`를 거친다. 상태 이력(`statushistory`)의 처리자 이름은 당시 `common.statushistory`가 auth 엔티티를 참조하게 되므로 조인하지 않았다. 패키지는 ADR-015로 분리되었고, 이름 조인은 #213에서 같은 방식(`left join`)으로 적용했다.

## 영향
- 조회 전용 리포지토리(InventoryQueryRepository/InventoryLotJpaRepository)는 다른 도메인의 JPA 엔티티(ProductSkuJpaEntity, WarehouseSectionJpaEntity, WarehouseJpaEntity)를 JPQL에서 직접 참조한다. 이는 명령 경로의 "ID만 참조" 원칙과 다르므로, 코드리뷰 시 "조회 전용인지" 구분해서 봐야 한다.
- 다른 도메인의 테이블/컬럼명이 바뀌면 이 JPQL들도 함께 깨지므로, 도메인 간 결합이 조회 경로에 한해 존재한다는 점을 인지해야 한다.
- 향후 도메인을 별도 서비스/DB로 분리한다면, 조회 전용 JOIN 쿼리부터 먼저 손봐야 한다.
