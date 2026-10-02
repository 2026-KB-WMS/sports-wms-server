# ADR-008 재고 도메인은 가드 예외(도메인 내부)와 사용자 응답 오류(서비스 계층 ErrorCode)를 2단으로 분리

## 상태
`Accepted` (2026-09-24)

## 컨텍스트
Product/Warehouse 도메인은 지금까지 서비스 계층에서 비즈니스 규칙을 검증하고 실패 시 곧장 BusinessException(도메인별 ErrorCode)을 던지는 단일 계층 방식이었다. 재고 도메인(InventoryLot)은 수량 불변식(0 <= allocated <= onHand, 가용 수량 부족 금지 등)이 많고, 이 불변식은 도메인 객체 자신이 지켜야 할 규칙이기도 하다. 도메인 객체에 아무 가드도 없이 서비스 계층 검증만 믿으면, 검증을 빠뜨린 새 호출부가 생겼을 때 수량이 조용히 깨질 위험이 있다.

## 결정
InventoryLot 같은 도메인 엔티티의 increase/decrease/allocate/release/ship 메서드는 canAllocate/canDecrease/canRelease 등으로 자체 검증하고, 위반 시 IllegalArgumentException/IllegalStateException을 던진다(사용자에게 보여줄 메시지가 아닌, "여기까지 오면 안 된다"는 최후 방어선). 실제 사용자 응답용 검증(가용 재고 부족 등 업무 오류)은 서비스 계층에서 먼저 수행해 InventoryErrorCode 기반 BusinessException으로 던지고, 정상적인 흐름에서는 도메인 가드 예외가 절대 발생하지 않아야 한다.

## 근거
- 도메인 객체가 스스로 불변식을 강제하면, 서비스 계층의 검증 누락이나 새로운 호출 경로가 생겨도 수량이 잘못된 상태로 저장되는 사고를 막을 수 있다(방어적 설계).
- IllegalArgumentException/IllegalStateException은 "버그" 신호로 취급해, 사용자 친화적 메시지·HTTP 상태코드 매핑 책임(BusinessException/ErrorCode 몫)과 분리된다. GlobalExceptionHandler는 이 둘을 별도 매핑 없이 catch-all(Exception.class) 핸들러가 잡아 500 INTERNAL_ERROR로 응답하므로, 사용자에게 노출되는 4xx 업무 오류와 자연히 구분된다.
- canAllocate/canDecrease/canRelease 같은 조회 메서드를 서비스 계층에 노출해, 서비스가 "실패할지"를 미리 확인하고 사용자 메시지를 결정할 수 있게 한다.

## 검토했던 대안
- Product/Warehouse처럼 서비스 계층에서만 검증하고 도메인 객체는 무방비 setter/증감 메서드만 제공 — 기각. 재고는 수량 정합성이 가장 중요한 도메인이라, 서비스 계층 검증 누락 시 바로 데이터 손상으로 이어지는 리스크가 더 크다고 판단했다.
- 도메인 객체가 직접 BusinessException(InventoryErrorCode)을 던지게 통일 — 기각. 도메인 모듈이 common.exception(BusinessException, ErrorCode)에 의존하게 되어 순수 도메인 모델 원칙이 흐려지고, "버그(가드)"와 "정상적인 업무 오류"를 코드 레벨에서 구분하기 어려워진다.

## 영향
- 재고 서비스(InventoryStockUseCase/InventoryAdjustmentUseCase 구현체)는 도메인 메서드를 호출하기 전에 반드시 can* 메서드로 먼저 검증하고 InventoryErrorCode로 실패 응답을 만들어야 한다. 도메인 메서드가 예외를 던지는 상황이 실제로 발생하면 검증 누락 버그로 간주해 디버깅한다.
- 다른 도메인에 이 패턴을 적용할지는 개별 판단: 수량처럼 불변식이 명확하고 중요한 도메인에 한해 적용한다.
