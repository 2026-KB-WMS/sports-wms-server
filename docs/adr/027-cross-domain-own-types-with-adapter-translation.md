# ADR-027: 도메인 경계는 자기 타입을 두고 어댑터에서 변환한다 (출고 → 지점 발주부터 적용)

- 상태: Accepted
- 날짜: 2026-10-14

## 컨텍스트
도메인 간 호출은 상대 도메인의 `application.port.in`을 거치지만, 그 포트가 엔티티와 enum을 그대로 돌려줬다. 출고 서비스 3개(`StockAllocationService`, `OutboundService`, `OutboundFulfillmentService`)가 지점 발주의 `StoreOrder`, `StoreOrderLine`, `StoreOrderStatus`를 직접 import해 getter를 불렀다(ArchUnit 점검에서 위반 52건). 발주 모델이 바뀌면 출고 코드가 같이 깨지고, 지점 발주를 OMS로 분리할 때는 엔티티를 공유할 수 없다. DDD 관점에서도 출고가 발주의 모델을 그대로 쓰면 발주의 용어와 변경이 출고 안으로 새어 든다.

## 결정
- **출고는 지점 발주를 자기 타입으로만 다룬다.** `outbound.application.port.out.StoreOrderPort`가 발주 스냅샷(`OrderView`, `LineView`)과 수량 변경 타입(`LineQuantity`, `LinePicked`), 출고 자기 enum(`outbound.domain.enums.StoreOrderState`)을 정의한다. 출고 서비스·응답 DTO는 이 타입만 import한다.
- **변환은 어댑터 한 곳에서 한다.** `outbound.adapter.out.storeorder.StoreOrderFulfillmentAdapter`가 발주의 `port.in`을 호출하고 결과를 출고 타입으로 바꾼다. 출고 안에서 `storeorder` 타입을 아는 곳은 `adapter.out.storeorder` 패키지뿐이다.
- **발주의 `port.in`은 엔티티를 밖으로 내보내지 않는다.** `StoreOrderFulfillmentUseCase`는 `port.in.result`의 읽기용 결과 객체(`StoreOrderFulfillmentOrderView`, `StoreOrderFulfillmentLineView`)를 돌려준다.
- **enum 변환은 이름으로 한다.** 발주 쪽에 상태가 추가되면 변환이 예외로 거절되므로(조용히 통과시키지 않는다) 출고 enum과 어댑터를 함께 갱신한다.
- **JSON 응답은 바꾸지 않는다.** `OutboundDeliverResponse.storeOrder.status`는 enum 이름이 같아 값이 그대로다. API 명세 변경 없음.
- **적용 범위**: 처음에는 출고 → 지점 발주 경계에 적용했고, 이어서 다른 도메인 `port.in`이 돌려주던 엔티티 직접 사용을 같은 방식으로 정리했다(#253). 타 도메인 enum 의존은 후속으로 정리한다.
- **enum 경계 규칙**: 다른 도메인이 쓰는 `port.in` 결과 객체와 command는 enum 대신 문자열 이름을 쓴다. 받는 쪽은 자기 enum을 정의하고 어댑터에서 `switch`로 명시 매핑한다(입고의 `StockQuality`·`SectionKind`, 지점·창고 멤버의 `MemberRole`, 계정의 배정 포트 `StoreOwnerAssignPort`·`WarehouseManagerAssignPort`는 역할 코드를 어댑터만 안다). 상대가 구분하지 않는 값은 `OTHER`로 모으고, 품질처럼 값이 닫혀 있으면 모르는 값을 거절한다. 같은 도메인 안의 웹 DTO·command·result는 enum을 유지한다(#255).
- **엔티티 대신 읽기용 결과 객체를 돌려준다.** 호출 쪽 어댑터가 쓰던 조회는 `port.in`에 결과 객체를 돌려주는 메서드를 추가해 쓴다: `ProductSkuUseCase.getSkuView`, `WarehouseUseCase.getWarehouseView`, `WarehouseSectionUseCase.getSectionView`, `StoreUseCase.getStoreView`, `UserUseCase.getUserView`. 엔티티를 돌려주는 기존 메서드는 자기 도메인 안에서 계속 쓴다. 호출 쪽은 이 결과 객체를 어댑터에서만 받고 자기 포트 타입으로 변환한다(예: 지점·창고 관리자 배정은 `StoreMemberUserPort`·`WarehouseMemberUserPort`의 `Candidate`를 쓰고, 인증 도메인의 `UserView` 변환은 `adapter.out.auth`가 맡는다). 다른 도메인이 쓰는 재고 반영 포트(`InventoryStockUseCase`)와 `LotUseCase.findOrRegister`는 반환 타입 자체를 `StockLotView`, `RegisteredLotView`로 바꿨다.

## 근거
- OMS 분리 시 어댑터 구현만 HTTP 호출 등으로 바꾸면 되고 출고 서비스는 건드리지 않는다. 경계를 모놀리스 안에서 먼저 코드로 드러내 두는 것이다.
- 발주 모델 변경이 어댑터 한 곳에서 흡수된다. 의미가 바뀌는 경우에만 출고가 영향을 받는다.
- 매핑 클래스와 타입이 늘어나는 비용이 있지만, 분리가 예정된 경계에서는 감수할 만하다.

## 검토했던 대안
- 발주의 결과 객체(`port.in.result`)를 출고 서비스가 그대로 사용: 기각. 엔티티 노출은 사라지지만 발주 용어가 출고 로직에 남아 분리 때 다시 바꿔야 한다.
- 모든 도메인 쌍에 일괄 적용: 보류. 매핑 보일러플레이트가 크고 분리 가능성이 없는 경계는 이점이 작다.
- ArchUnit 동결 목록으로 위반을 두고 새 위반만 막기: 보류. 이 경계는 위반 자체를 없애는 쪽을 택했다.

## 영향
- 코드: `StoreOrderPort`, `StoreOrderState`, `StoreOrderFulfillmentAdapter` 추가. 출고 서비스 3개, `StoreOrderOutboundAdapter`, `OutboundDeliverResult`·`OutboundDeliverResponse` 수정. 발주의 `StoreOrderFulfillmentUseCase`·서비스 반환 타입 변경.
- **사용 여부 확인은 소유 도메인이 판단한다**: 상품·SKU·창고 비활성화 가드의 "진행 중 업무가 있는가"는 입고·출고·지점 발주가 `InboundUsageUseCase`, `OutboundUsageUseCase`, `StoreOrderUsageUseCase`로 판단해 결과만 돌려준다. 진행 중 상태 집합은 각 소유 도메인의 서비스가 갖고, `ProductUsageAdapter`·`WarehouseUsageAdapter`는 상태 enum을 쓰지 않는다(#257). 재고 존재·구역 참조 확인의 JPQL은 ADR-007 그대로 남는다.
- 남은 의존: `StoreOrderOutboundAdapter`는 발주가 정의한 `port.out`(`StoreOrderOutboundPort`)을 구현하므로 발주 타입 5건이 남는다. 방향 반전(발주가 출고의 `port.in`을 호출)은 도메인 간 순환을 만들 수 있어 이번에는 하지 않았다.
- 테스트: 어댑터 단위 테스트와 출고 통합 테스트의 타입을 맞췄고 동작 변경은 없다.
