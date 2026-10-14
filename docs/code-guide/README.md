# 코드 가이드

`src/main/java/com/kb/wms/<도메인>/`의 패키지 구조를 따라 클래스별 필드·메서드·쓰임을 정리한 문서다. 같은 경로의 `.md`가 같은 경로의 `.java`에 대응한다.

- 코드를 읽기 전에 **무엇을 하는 클래스인지**, 코드를 읽은 뒤에 **빠뜨린 규칙이 없는지** 확인하는 용도다.
- 이 문서는 코드를 읽고 정리한 **파생 문서**다. 명세의 기준은 `docs/api/`, `docs/domain/`, `docs/adr/`이고, 코드와 다르면 코드를 기준으로 본 내용을 적었다.
- 각 문서의 "관찰" 절에는 코드를 읽다가 발견한 명세·주석과 구현의 차이를 적어 두었다(코드는 수정하지 않았다).

## 도메인별 문서

| 도메인 | 시작 문서 | 한 줄 요약 |
|---|---|---|
| common | `common/README.md` | 오류 코드, 공통 응답, JWT 인증, 역할별 경로 규칙, 거점 코드 발급 |
| product | `product/README.md` | 브랜드·카테고리·상품·SKU·옵션, 상품·SKU 코드 자동 발급 |
| warehouse | `warehouse/README.md` | 창고, 계층형 구역(수용량), 창고 관리자 배정 |
| store | `store/README.md` | 지점, 점주 배정, 상태 이력 기록 |
| inventory | `inventory/README.md` | 구역+로트 재고 원장, 수량 변동 이력, 로트 상태 |
| inbound | `inbound/README.md` | 공급처, 창고 발주(→공급처), 입고(검수·완료) |
| storeorder | `storeorder/README.md` | 지점 발주(지점→창고) 상태 전이와 승인·배정·보류·종결 |
| outbound | `outbound/README.md` | 재고 할당(FEFO), 출고(피킹·배송) |
| statushistory | `statushistory/README.md` | 상태 전이 이력(공용) |
| auth | `auth/README.md` | 가입·로그인·계정 관리·승인과 소속 배정 |

## 도메인 사이의 호출 방향

```
auth ──(UserUseCase)──▶ warehouse, store  (소속 배정)
inbound ──(포트·어댑터)──▶ inventory (입고 반영, 로트), warehouse (구역), product (매입 단가)
storeorder ◀──(StoreOrderFulfillmentUseCase)── outbound ──▶ inventory (할당·차감)
storeorder ──(StoreOrderOutboundPort, OutboundOrderLinkUseCase)──▶ outbound
store ──(StoreOrderPresencePort)──▶ storeorder
warehouse, product ──(UsagePort, 소유 도메인의 UsageUseCase)──▶ inbound, outbound, storeorder
모든 도메인 ──▶ common, statushistory
```

도메인 사이의 직접 의존은 **포트 + 어댑터 한 곳**으로 모은다. 순환 의존이 생기는 곳(지점↔발주↔출고, 계정↔소속 배정)은 UseCase를 나눠 끊었다.

## 문서 작성 규칙

| 패키지 | 문서 단위 |
|---|---|
| `domain/entity` | 클래스별 |
| `domain/enums` | 패키지당 `Enums.md` 하나 |
| `exception` | 오류 코드 enum(+제약 대응표) 문서 |
| `application/service` | 클래스별 |
| `application/port/in` | `UseCases.md`, `command/Commands.md`, `query/SearchConditions.md`, `result/Results.md` |
| `application/port/out` | `Repositories.md` 또는 `Ports.md` |
| `adapter/in/web` | 컨트롤러별(또는 `Controllers.md`), `dto/request/Requests.md`, `dto/response/Responses.md` |
| `adapter/out` | 도메인 연결 `Adapters.md`, `persistence/` 아래 어댑터·리포지토리·엔티티 |

Command, 검색 조건, Result, 요청·응답 DTO는 클래스마다 **용도, 엔드포인트, 만드는 곳, 쓰는 곳**을 적는다.
