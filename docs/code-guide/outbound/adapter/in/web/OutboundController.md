# OutboundController

`outbound/adapter/in/web/OutboundController.java` · `@RestController` · `/api/v1/outbounds` · Swagger 태그 "출고"

처리 사용자는 토큰 주체다. 역할(쓰기는 창고 관리자, 조회는 본사와 창고 관리자)은 `SecurityConfig`가, 담당 창고 범위는 서비스가 검사한다.

## 의존

`OutboundUseCase`, `OutboundFulfillmentUseCase`

## 엔드포인트

| 메서드 | 경로 | 상태 | 쿼리·바디 | 응답 | 호출 UseCase | 허용 역할 |
|---|---|---|---|---|---|---|
| `createOutbound` | POST `/` | 201 | `OutboundCreateRequest` | `OutboundCreateResponse` | `OutboundUseCase.createOutbound` | WAREHOUSE_MANAGER |
| `getOutbounds` | GET `/` | 200 | `status`, `warehouseId`, `storeId`, `storeOrderId`, `keyword`, `createdFrom`, `createdTo`, `page`, `size`, `sort` | `PageResponse<OutboundListItemResponse>` | `searchOutbounds`(정렬 필드: `createdAt`, `shippedAt`, `status`, `outboundNo`, `storeName`, `warehouseName`, `lineCount`) | HQ_ADMIN, WAREHOUSE_MANAGER |
| `getOutboundDetails` | GET `/{outboundId}/details` | 200 | - | `OutboundDetailResponse` | `getOutbound` | HQ_ADMIN, WAREHOUSE_MANAGER |
| `startPicking` | PATCH `/{outboundId}/picking/start` | 200 | - | `OutboundPickingStartResponse` | `OutboundUseCase.startPicking` | WAREHOUSE_MANAGER |
| `completePicking` | PATCH `/{outboundId}/picking/complete` | 200 | `OutboundPickingCompleteRequest` | `OutboundPickingCompleteResponse` | `OutboundFulfillmentUseCase.completePicking` | WAREHOUSE_MANAGER |
| `ship` | PATCH `/{outboundId}/ship` | 200 | - | `OutboundShipResponse` | `OutboundFulfillmentUseCase.ship` | WAREHOUSE_MANAGER |
| `deliver` | PATCH `/{outboundId}/deliver` | 200 | - | `OutboundDeliverResponse` | `OutboundFulfillmentUseCase.deliver` | WAREHOUSE_MANAGER |
| `cancelOutbound` | PATCH `/{outboundId}/cancel` | 200 | `OutboundReasonRequest` | `OutboundCancelResponse` | `OutboundUseCase.cancel` | WAREHOUSE_MANAGER |

출고 단건 조회는 `GET /{outboundId}`가 아니라 **`/{outboundId}/details`** 하나뿐이다.

`cancelOutbound`는 경로 ID, 바디의 `reason`, 토큰 사용자를 합쳐 `OutboundCancelCommand`를 직접 만든다.
