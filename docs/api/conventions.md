# 공통 API 규칙

모든 엔드포인트가 따르는 공통 규칙이다. 이 문서가 기준(source of truth)이며, 2026-10-02에 Notion "공통 API 규칙"(최종 수정 2026-09-24)에서 이전했다. 도메인별 엔드포인트 명세의 "검증 및 비즈니스 규칙"에는 여기에 없는 도메인 특수 규칙만 추가로 적는다. 비기능 요구사항(성능·동시성·보안 등)은 별도 문서(Notion "비기능 요구사항")에서 관리한다.

> **현재 구현 기준 메모** (결정 사항, 아래 규칙과 다르면 이 메모가 우선)
> - 페이지네이션은 도메인별로 필요한 목록에만 적용한다([ADR-032](../adr/032-pagination-and-sorting.md), #287). 적용한 목록은 `data.items`와 `data.pageInfo`를 돌려주고, 적용하지 않은 목록은 지금처럼 `data.items`만 돌려주며 `page`·`size`·`sort`를 받지 않는다(보내도 무시). 어느 목록이 적용 대상인지는 각 도메인 API 문서가 기준이다.
> - 리소스 404는 도메인 전용 코드를 쓴다 (예: `PRODUCT_NOT_FOUND`, `SUPPLIER_NOT_FOUND`).

## 기본 정보 공통 규칙
- Base path: `/api/v1`, 버전은 `v1`로 고정한다.
- `도메인`은 인증 / 상품 / 창고 / 입고 / 재고 / 출고 / 발주 / 지점 중 하나로 API 명세 속성과 동일하게 맞춘다.
- `인증`은 `없음`(가입·로그인) / `Bearer 토큰`(일반 인증 필요) / `관리자 권한`(`HQ_ADMIN` 전용) 중 하나를 기재한다.
- 리소스 경로는 복수형 명사를 사용하고(`/purchase-orders`), 상태 전이는 `PATCH /{resource}/{id}/{action}` 형식을 따른다 (예: `PATCH /purchase-orders/{id}/confirm`).
- 날짜·시각은 ISO-8601 형식이지만 **오프셋(`Z`, `+09:00`)을 붙이지 않는다.** `YYYY-MM-DDTHH:mm:ss`(예: `2026-10-05T14:30:00`)로 주고받고, 소수 초는 값이 있을 때만 붙는다(예: `2026-10-05T14:30:00.123456`). DTO가 `LocalDateTime`이고 `@JsonFormat` 설정이 따로 없어 Spring Boot 기본 직렬화(숫자 타임스탬프 아님)를 그대로 쓰기 때문이다. 값은 서버 시간대 KST(Asia/Seoul) 기준 시각이며(컨테이너 `TZ`, `-Duser.timezone`, `spring.jackson.time-zone`, `hibernate.jdbc.time_zone`, JDBC `serverTimezone` 모두 Asia/Seoul), 클라이언트가 시간대를 변환해 주지 않는다. 날짜만 있는 값(`expiryDate`, `manufacturedDate`)은 `YYYY-MM-DD`다. 요청 본문·쿼리 파라미터(`createdFrom`, `arrivedTo` 등)도 같은 오프셋 없는 형식을 쓴다. **서버 시간대는 KST(Asia/Seoul)로 고정**이며, 오프셋 없는 `yyyy-MM-dd'T'HH:mm:ss`로 직렬화한다.
- **전화번호 형식**: 사용자 `phone`, 창고·지점·공급처 `contactNumber`는 하이픈을 포함한 `지역번호(2~3자리)-국번(3~4자리)-4자리`(`^\d{2,3}-\d{3,4}-\d{4}$`)만 받는다(예: `010-1234-5678`, `02-123-4567`, `031-1234-5678`). 형식이 틀리면 400 `VALIDATION_ERROR`이고, 이미 저장된 값은 변환하지 않는다. 1588-XXXX 같은 대표번호는 받지 않는다.
- **위도·경도**: 주소를 입력받는 창고·지점·공급처는 선택 필드 `latitude`(-90~90), `longitude`(-180~180)를 받는다. 쌍으로만 보낼 수 있고(한쪽만 보내면 400 `VALIDATION_ERROR`), 소수 7자리로 반올림해 저장한다. 값이 없는 기존 데이터는 응답에서 `null`이다. 수정 요청에서 생략하면 기존 좌표를 유지하며, 좌표를 비우는 방법은 없다 ([ADR-020](../adr/020-geo-coordinates-optional-pair.md)).

## 요청 공통 규칙

### Headers

| 헤더 | 필수 | 설명 |
|---|---|---|
| Authorization | 조건부 | `Bearer {accessToken}`. 인증 항목이 `없음`인 API(가입·로그인)는 생략한다. |
| Content-Type | 조건부 | `application/json` (Body가 있는 POST/PATCH/PUT에 필수) |

- 토큰은 로그인(`POST /auth/login`)이 발급하는 액세스 토큰(JWT, HS256)뿐이며 만료는 3600초다. 리프레시 토큰은 없다 ([ADR-011](../adr/011-jwt-access-token-only.md), [auth.md](auth.md)).

### Query Parameters (목록 조회 공통)
- 페이지네이션을 적용한 목록 API만 아래 파라미터를 받는다 (적용 여부는 각 도메인 API 문서 기준, 위 구현 기준 메모 참고). 적용하지 않은 목록은 받지 않고(보내도 무시된다) 엔드포인트마다 고정된 정렬을 쓴다.
  - `page`: 1부터 시작, 기본 1.
  - `size`: 기본 20, 1~100.
  - `sort`: `필드,asc|desc` 형식 하나(예: `createdAt,desc`). 방향을 생략하면 asc, 대소문자는 구분하지 않는다. 정렬할 수 있는 필드는 API마다 정해져 있고 각 명세의 "정렬" 항목에 적는다. 생략하면 그 API의 기본 정렬을 쓴다.
  - 범위를 벗어난 `page`·`size`, 허용되지 않은 `sort` 필드·방향·형식(정렬 기준 둘 이상 포함)은 400 `VALIDATION_ERROR`다.
  - 어떤 정렬이든 마지막에 ID를 첫 정렬과 같은 방향으로 붙여 순서를 고정한다(같은 값이 많아도 페이지 사이에 행이 겹치거나 빠지지 않는다).
  - 기본 정렬은 각 명세의 "정렬" 항목에 적는다.
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
- 목록 조회 API는 `data.items`(배열)를 포함한다. 페이지네이션을 적용한 목록은 `data.pageInfo`도 포함한다.
  ```json
  "data": {
    "items": [ ],
    "pageInfo": { "page": 1, "size": 20, "totalElements": 135, "totalPages": 7, "sort": "createdAt,desc" }
  }
  ```
  `totalElements`는 조건에 맞는 전체 건수, `totalPages`는 올림한 전체 페이지 수(항목이 없으면 0)다. `sort`는 실제 적용된 첫 정렬 기준이며, 요청에 `sort`가 없으면 기본 정렬이 내려가므로 화면이 정렬 화살표를 그릴 때 쓴다. 범위를 넘어선 `page`는 `items`가 빈 배열이고 `pageInfo`는 그대로 채워진다.
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
- [x] 액세스 토큰 발급/만료 정책 (ADR-011: 액세스 토큰만, 3600초, 갱신 없음). 리프레시 토큰·즉시 무효화는 보류
- [ ] Rate limiting 적용 여부 (현재 없음, 보류)
- [ ] 도메인 특수 오류 코드 전체 목록을 API 명세 각 엔드포인트에 반영
