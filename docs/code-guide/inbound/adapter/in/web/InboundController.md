# InboundController

`inbound/adapter/in/web/InboundController.java` · `@RestController` · `/api/v1/inbounds` · Swagger 태그 "입고"

처리 사용자는 토큰 주체다. 역할은 `SecurityConfig`가, 담당 창고 범위는 서비스가 검사한다. 목록은 페이지네이션 없이 전체를 반환한다.

## 의존

`InboundUseCase`, `InboundInspectUseCase`, `InboundCompleteUseCase`

## 엔드포인트

| 메서드 | 경로 | 상태 | 쿼리·바디 | 응답 | 설명 | 허용 역할 |
|---|---|---|---|---|---|---|
| `registerInbound` | POST `/` | 201 | `InboundRegisterRequest` | `InboundRegisterResponse` | 입고(도착) 등록 | WAREHOUSE_MANAGER |
| `getInbounds` | GET `/` | 200 | `status`, `warehouseId`, `purchaseOrderId`, `keyword`, `arrivedFrom`, `arrivedTo` | `ItemsResponse<InboundSummaryResponse>` | 목록 | HQ_ADMIN, WAREHOUSE_MANAGER |
| `getInbound` | GET `/{inboundId}` | 200 | - | `InboundResponse` | 헤더 단건 | HQ_ADMIN, WAREHOUSE_MANAGER |
| `getInboundDetails` | GET `/{inboundId}/details` | 200 | - | `InboundDetailsResponse` | 검수 항목 포함 상세 | HQ_ADMIN, WAREHOUSE_MANAGER |
| `inspectInbound` | PATCH `/{inboundId}/inspect` | 200 | `InboundInspectRequest` | `InboundInspectResponse` | 검수 (항목 전체 교체) | WAREHOUSE_MANAGER |
| `completeInbound` | PATCH `/{inboundId}/complete` | 200 | - | `InboundCompleteResponse` | 입고 완료 (재고 반영) | WAREHOUSE_MANAGER |
| `cancelInbound` | PATCH `/{inboundId}/cancel` | 200 | `InboundCancelRequest` | `InboundCancelResponse` | 취소 | WAREHOUSE_MANAGER |
| `getAssignableSections` | GET `/{inboundId}/assignable-sections` | 200 | `requiredQuantity`, `keyword` | `SectionCandidatesResponse` | 합격품 구역 후보 | HQ_ADMIN, WAREHOUSE_MANAGER |
| `getDefectSections` | GET `/{inboundId}/defect-sections` | 200 | `requiredQuantity`, `keyword` | `SectionCandidatesResponse` | 불량품 구역 후보 | HQ_ADMIN, WAREHOUSE_MANAGER |

허용 역할: `SecurityConfig`에서 GET `/inbounds/**`는 HQ_ADMIN·WAREHOUSE_MANAGER, 그 밖의 `/inbounds/**`는 WAREHOUSE_MANAGER다. 구역 후보 GET도 `GET /inbounds/**`에 속한다(본사는 서비스의 `requireWarehouseAccess`를 통과한다).

## 응답 조립

| 엔드포인트 | 조립 방식 |
|---|---|
| 등록 | `registerInbound`가 ID만 반환 → `getInbound`로 뷰를 다시 조회해 응답 |
| 검수 | 검수 결과 `Inbound` + `getInboundDetails`의 항목 |
| 취소 | 취소된 `Inbound` + `getInbound`(발주 상태·취소 사유) |
| 구역 후보 | `getInbound`로 창고 ID를 얻어 `SectionCandidatesResponse.of(inboundId, warehouseId, items)` |
