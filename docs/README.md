# docs 색인

이 프로젝트의 명세/결정 문서는 **레포의 `docs/`를 기준(source of truth)** 으로 한다. 아직 옮기지 않은 문서만 Notion에 있으며, 도메인 구현을 시작할 때 해당 도메인 문서를 `docs/`로 옮긴다.

## 기준 위치

| 문서 | 기준 | 위치 | 비고 |
|---|---|---|---|
| ADR (기술 결정 기록) | **레포** | [`adr/`](adr/README.md) | 2026-10-02 이전 완료. 이후 레포에서 계속 추가(현재 ADR-001~014) |
| 공통 API 규칙 | **레포** | [`api/conventions.md`](api/conventions.md) | 2026-10-02 이전 완료 |
| 입고 도메인 상태 전이/권한/부수 효과 | **레포** | [`domain/inbound.md`](domain/inbound.md) | 2026-10-02 기준 문서로 승격(구현 완료). 구현 현황·보류 항목 포함. Notion "업무 상태 전이도"의 입고 부분은 참고용 |
| 지점 발주 도메인 상태 전이/권한/부수 효과 | **레포** | [`domain/store-order.md`](domain/store-order.md) | 2026-10-03 기준 문서로 이전(구현 시작 시점 기준, 이후 구현 완료). 항목 상태·점주용 `progressStage` 포함. Notion "업무 상태 전이도"의 지점 발주 부분은 참고용 |
| 출고·재고 할당 도메인 상태 전이/권한/부수 효과 | **레포** | [`domain/outbound.md`](domain/outbound.md) | 2026-10-05 기준 문서로 이전(구현 시작 시점 기준, 이후 구현 완료). 발주 연동 교체 목록과 결정·미결 포함. Notion "업무 상태 전이도"의 재고 할당·출고 부분은 참고용 |
| 업무 상태 전이도 (반품) | Notion | WMS 문서 관리 → 업무 상태 전이도 | MVP 범위 밖(2026-09-21 확정). 재진입 시 이전 |
| API 명세: 상품 / 창고 / 재고 / 지점 | **레포** | [`api/product.md`](api/product.md), [`warehouse.md`](api/warehouse.md), [`inventory.md`](api/inventory.md), [`store.md`](api/store.md) | 2026-10-02 이전 완료(구현된 도메인). 각 파일 상단 "구현 대비 메모" 참고 |
| API 명세: 입고 (공급처·창고 발주·입고) | **레포** | [`api/inbound.md`](api/inbound.md) | 2026-10-02 이전 완료(Supplier 5 + PurchaseOrder 6 + Inbound 9). 상단 "구현 대비 메모" 참고 |
| API 명세: 지점 발주 | **레포** | [`api/store-order.md`](api/store-order.md) | 2026-10-03 이전 완료(12개, 구현 완료). 재고 할당 `/allocations`는 제외(출고 도메인으로 이전). 상단 "구현 대비 메모" 참고 |
| API 명세: 출고·재고 할당 | **레포** | [`api/outbound.md`](api/outbound.md) | 2026-10-05 이전 완료(출고 8 + 재고 할당 4 = 12개, 구현 완료). 상단 "구현 대비 메모" 참고 |
| 인가 규칙·역할 매트릭스 | **레포** | [`api/authorization.md`](api/authorization.md) | 2026-10-08 작성(#169). 도메인 API 명세의 권한 항목을 한 표로 모음. 불일치하면 도메인 명세가 기준 |
| API 명세: 인증 (가입·로그인·내 정보·계정 관리) | **레포** | [`api/auth.md`](api/auth.md) | 2026-10-08 이전 완료(구현 6개, 비밀번호 변경 #193 추가; Notion의 `GET /users/{userId}`는 미구현). 상단 "구현 대비 메모" 참고 |
| 인증·계정 도메인 상태 전이/권한/부수 효과/토큰 정책 | **레포** | [`domain/auth.md`](domain/auth.md) | 2026-10-08 기준 문서로 작성(구현 완료). 최초 본사 관리자 생성(ADR-013)과 보류 항목 포함 |
| 비활성화 가능/불가 조건 (창고·구역·지점·공급처·상품·SKU·계정) | **레포** | [`domain/deactivation.md`](domain/deactivation.md) | 2026-10-10 코드 기준 정리. 명세와 코드 차이는 문서 하단 참고 |
| 단가 기준 (SKU 현재 단가·발주·입고·로트 원가·지점 발주·출고 확정 단가) | **레포** | [`domain/pricing.md`](domain/pricing.md) | 2026-10-10 코드 기준 정리. 단가 7종의 저장 위치·결정 시점·노출 범위와 알려진 공백 포함 |
| ERD 데이터 사전 / ERD 개요 | Notion | WMS 문서 관리 | 도메인 구현 시 이전 검토. 컬럼 정의는 Flyway SQL이 실제 기준 |
| 기능 명세, 유스케이스, 사용자별 요구사항, 비기능 명세, 프로젝트 개요, 화면 설계 | Notion | WMS 문서 관리 | 이전 계획 없음 (기획 문서) |
| 개발 일정, [보류] 항목 | Notion | WMS 개발 일정 DB | 진행 관리용, 이전하지 않음 |

## 이전 규칙
1. 문서를 옮기면 이 표의 기준 열을 **레포**로 바꾼다.
2. Notion 원본 페이지 맨 위에 "레포 `docs/...`로 이전됨" 안내와 링크를 남기고, 내용 갱신은 레포에서만 한다.
3. 옮긴 문서 상단에는 이전 날짜와 Notion 원본 수정일을 적는다.
4. 명세와 코드가 다르면 임의로 한쪽을 고치지 말고 불일치를 먼저 보고한다 (`spec-reviewer`).
5. 이전 시점에 발견한 차이는 각 문서 상단 "구현 대비 메모"에 기록한다. 알려진 차이: 상품 `PATCH /products/skus/{skuId}/status`는 코드에만 있음(명세는 `api/product.md`에 보강함), 창고 `DELETE /warehouses/sections/{sectionId}`는 명세에만 있음(미구현), Notion 상태 컬럼이 실제 구현 상태와 다름(지점·입고 등), Notion의 `pageInfo`·일반 `NOT_FOUND`는 현재 구현 기준(페이지네이션 보류, 도메인별 404)과 다름. 출고는 Notion 개발 일정 페이지가 처음에 7개만 적고 있었으나(출고 취소·재고 할당 4개 누락) 지금은 명세와 같은 12개로 갱신되어 있다. 인증은 Notion의 `GET /users/{userId}`가 코드에 없다([`api/auth.md`](api/auth.md) 메모).

## 구조
```
docs/
├── README.md          # 이 문서
├── adr/               # 기술 결정 기록
├── api/               # 공통 규칙 + (이전된) 도메인별 API 명세
└── domain/            # 상태 전이, 권한, 부수 효과
```
