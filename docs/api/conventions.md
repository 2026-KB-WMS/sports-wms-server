# 공통 API 규칙

모든 엔드포인트가 따르는 공통 규칙이다. 이 문서가 기준(source of truth)이며, 2026-10-02에 Notion "공통 API 규칙"(최종 수정 2026-09-24)에서 이전했다. 도메인별 엔드포인트 명세의 "검증 및 비즈니스 규칙"에는 여기에 없는 도메인 특수 규칙만 추가로 적는다. 비기능 요구사항(성능·동시성·보안 등)은 별도 문서(Notion "비기능 요구사항")에서 관리한다.

> **현재 구현 기준 메모** (결정 사항, 아래 규칙과 다르면 이 메모가 우선)
> - 페이지네이션은 보류 중이다. 목록 API는 `data.items`만 반환하고(`data.pageInfo`는 아직 없음), 페이징은 전 도메인에 한꺼번에 추가한다.
> - 리소스 404는 도메인 전용 코드를 쓴다 (예: `PRODUCT_NOT_FOUND`, `SUPPLIER_NOT_FOUND`).

## 기본 정보 공통 규칙
- Base path: `/api/v1`, 버전은 `v1`로 고정한다.
- `도메인`은 인증 / 상품 / 창고 / 입고 / 재고 / 출고 / 발주 / 지점 중 하나로 API 명세 속성과 동일하게 맞춘다.
- `인증`은 `없음`(가입·로그인) / `Bearer 토큰`(일반 인증 필요) / `관리자 권한`(`HQ_ADMIN` 전용) 중 하나를 기재한다.
- 리소스 경로는 복수형 명사를 사용하고(`/purchase-orders`), 상태 전이는 `PATCH /{resource}/{id}/{action}` 형식을 따른다 (예: `PATCH /purchase-orders/{id}/confirm`).
- 날짜·시각은 ISO-8601 형식이지만 **오프셋(`Z`, `+09:00`)을 붙이지 않는다.** `YYYY-MM-DDTHH:mm:ss`(예: `2026-10-05T14:30:00`)로 주고받고, 소수 초는 값이 있을 때만 붙는다(예: `2026-10-05T14:30:00.123456`). DTO가 `LocalDateTime`이고 `spring.jackson.*`·`@JsonFormat` 설정이 따로 없어 Spring Boot 기본 직렬화(숫자 타임스탬프 아님)를 그대로 쓰기 때문이다. 값은 서버(JVM 기본 시간대)의 로컬 시각이며(개발 DB 연결은 `serverTimezone=Asia/Seoul`), 클라이언트가 시간대를 변환해 주지 않는다. 날짜만 있는 값(`expiryDate`, `manufacturedDate`)은 `YYYY-MM-DD`다. 요청 본문·쿼리 파라미터(`createdFrom`, `arrivedTo` 등)도 같은 오프셋 없는 형식을 쓴다. UTC 저장·시간대 정책은 확정 필요 항목이다.

## 요청 공통 규칙

### Headers

| 헤더 | 필수 | 설명 |
|---|---|---|
| Authorization | 조건부 | `Bearer {accessToken}`. 인증 항목이 `없음`인 API(가입·로그인)는 생략한다. |
| Content-Type | 조건부 | `application/json` (Body가 있는 POST/PATCH/PUT에 필수) |

- 액세스·리프레시 토큰 발급 방식과 만료 시간은 TBD — 확정 시 이 문서에 반영한다.

### Query Parameters (목록 조회 공통)
- `page`(1부터 시작, 기본 1), `size`(기본 20, 최대 100), `sort`(예: `createdAt,desc`) — **페이지네이션 도입 시 적용** (위 구현 기준 메모 참고). 현재는 어떤 목록 API도 `page`·`size`·`sort`를 받지 않고(보내도 무시된다) 엔드포인트마다 고정된 정렬을 쓴다. 기본 정렬은 각 명세의 "정렬" 항목에 적는다.
- 일시 범위 필터(`createdFrom`/`createdTo`, `arrivedFrom`/`arrivedTo`, `requestedFrom`/`requestedTo`)는 양 끝을 모두 포함한다(`>= from`, `<= to`). `expiringBefore`도 당일을 포함한다(`<=`).
- 도메인별 필터 파라미터(예: `skuId`, `sectionId`, `expiringBefore`)는 각 엔드포인트 명세의 "Query Parameters"에서 개별 정의하고, 여기서 정한 페이지네이션 파라미터 이름은 그대로 재사용한다.

## 응답 공통 규칙

### 성공
```json
{
  "success": true,
  "statusCode": 200,
  "message": "요청에 성공하였습니다.",
  "data": { }
}
```
- 목록 조회 API는 `data.items`(배열)를 포함한다. 페이지네이션 도입 후에는 `data.pageInfo`(`page`, `size`, `totalElements`, `totalPages`)도 포함한다.
- 생성(201) 응답은 `statusCode`가 `201`, `message`가 `"생성되었습니다."`이고 나머지 구조는 같다.
- JSON 필드명은 요청·응답 모두 camelCase로 통일한다(`statusCode`, `errorCode`, `pageInfo`, `totalElements`, `totalPages` 등). 스네이크 케이스 필드는 쓰지 않는다. (2026-09-24 결정)

### 오류
```json
{
  "success": false,
  "statusCode": 400,
  "message": "입력값을 확인해주세요.",
  "errorCode": "VALIDATION_ERROR",
  "errors": [
    { "field": "quantity", "reason": "1 이상이어야 합니다" }
  ]
}
```
- `errors`는 필드 단위 검증 오류가 있을 때만 채우고, 없으면 빈 배열로 둔다.
- 오류 코드는 아래 6종을 기본 세트로 모든 엔드포인트에 적용한다. 도메인 특수 오류가 필요하면 이 표를 확장하지 않고, 해당 엔드포인트 명세의 "검증 및 비즈니스 규칙"에 개별 `errorCode`를 추가한다.

| HTTP 상태 | 오류 코드 | 조건 | 처리 방법 |
|---|---|---|---|
| 400 | VALIDATION_ERROR | 입력값 검증 실패 | 필드별 오류 수정 |
| 401 | UNAUTHORIZED | 토큰 없음·만료 | 재인증 |
| 403 | FORBIDDEN | 권한·소속 범위 부족 (예: 창고 관리자가 마스터 데이터 수정 시도) | 역할·소속 범위 확인 |
| 404 | NOT_FOUND | 대상 리소스 없음 | 식별자 확인 |
| 409 | CONFLICT | 중복 요청·동시성 충돌·상태 전이 조건 불충족 (예: `CONFIRMED`가 아닌 발주에 입고 등록 시도) | 현재 상태 재조회 후 재시도 |
| 500 | INTERNAL_ERROR | 처리되지 않은 서버 오류(메시지 "서버 내부 오류가 발생했습니다.") | 잠시 후 재시도, 계속되면 서버 로그 확인 |

## 권한 검증 공통 규칙
- 모든 API는 JWT에서 역할(`HQ_ADMIN`/`WAREHOUSE_MANAGER`/`STORE_OWNER`)과 소속(창고/지점 ID)을 추출해 (1) 역할이 해당 API를 호출할 수 있는지, (2) 리소스가 요청자의 소속 범위 안에 있는지를 검증한다. 위반 시 403 `FORBIDDEN`.
- 마스터 데이터(상품·창고·구역·지점·공급처·멤버) 등록·수정·비활성화 API는 `HQ_ADMIN`만 호출 가능하다 ([ADR-003](../adr/003-master-data-owned-by-hq.md), 업무 상태 전이도의 "마스터 데이터 관리 주체" 표 기준).
- 상태 전이 API의 수행 역할·조건은 도메인마다 다르므로 각 엔드포인트 명세에서 개별 정의하되, 상태 코드 자체는 업무 상태 전이도를 단일 출처로 한다.

## 상태 전이 API 공통 규칙
- 이미 전이된 상태에 같은 액션이 재요청되면 409 `CONFLICT`로 응답하고 부작용을 재실행하지 않는다 (멱등하지 않은 전이는 명시적으로 막는다).
- 재고 수량·할당이 바뀌는 전이(할당·해제, 피킹 완료, 입고 완료 등)는 DB 트랜잭션으로 묶어 원자적으로 처리한다. 락 전략은 [ADR-006](../adr/006-inventory-pessimistic-lock.md) 참고.
- 업무 상태를 바꾸는 모든 API는 같은 트랜잭션에서 `StatusHistory`에 이전·이후 상태, 사유(있는 경우), 처리자, 시각을 기록한다. 재고 수량 변동은 `InventoryTransaction`이 별도로 기록한다.

## 확정 필요 항목
- [ ] 액세스·리프레시 토큰 발급/만료/갱신 정책
- [ ] Rate limiting 적용 여부
- [ ] 시간대 정책(현재는 오프셋 없는 서버 로컬 시각, UTC 저장·오프셋 포함 직렬화 여부)
- [ ] 도메인 특수 오류 코드 전체 목록을 API 명세 각 엔드포인트에 반영
