# WMS Server (sports-wms-server)

스포츠 용품 WMS(창고 관리 시스템) 백엔드. 코드를 읽어서 알 수 있는 내용은 적지 않고, **결정 사항과 금지 사항**만 기록한다.

## 스택 / 실행

- Java 21, Spring Boot (Gradle), 기본 패키지 `com.kb.wms`
- MySQL + Flyway (`src/main/resources/db/migration/V{n}__{snake_case}.sql`), 테스트는 H2
- 로컬 실행: `docker-compose up` (앱 + MySQL), 설정은 YAML(`application.yml`)
- 빌드/테스트: `./gradlew build`, `./gradlew test` (Windows는 `gradlew.bat`)

## 아키텍처

도메인별 패키지 + 도메인 내부 헥사고날(ports/adapters).

```
com.kb.wms.<domain>/
├── domain/entity/            # 엔티티 (enums와 분리)
├── domain/enums/             # 상태 enum
├── application/port/in/      # UseCase 인터페이스 (+ command / query / result)
├── application/port/out/     # Repository 포트
├── application/service/      # UseCase 구현
├── adapter/in/web/           # Controller (+ dto)
├── adapter/out/persistence/  # PersistenceAdapter (+ entity / repository)
└── exception/                # <Domain>ErrorCode
```

- 도메인: auth, inbound, inventory, outbound, product, statushistory, storeorder, store, warehouse. 공통 코드는 `common`(SecurityConfig, GlobalExceptionHandler, ApiResponse).
- 새 도메인/기능은 **product 도메인의 구조를 기준 예시**로 따른다.
- 패키지 매핑 주의: `inbound` = Supplier/PurchaseOrder/PurchaseOrderLine/Inbound/InboundLine (ERD "입고"), `storeorder` = StoreOrder/StoreOrderLine (ERD "발주", 지점→창고 요청). `inbound`의 PurchaseOrder(창고 발주)와 혼동하지 않는다.

## 코딩 규칙 (결정 사항)

각 규칙의 배경은 `docs/adr/`에 있다. 규칙을 바꾸려면 ADR부터 갱신한다.

- **엔티티 참조는 `Long xxxId`만** 사용한다. 같은 도메인/다른 도메인 모두 `@ManyToOne`, `@OneToMany` 등 JPA 연관관계 금지 ([ADR-005](docs/adr/005-entity-reference-by-id.md)). 예외: 조회 전용 API의 JPQL 조인([ADR-007](docs/adr/007-inventory-query-jpql-cross-domain-join.md)), 복합키 `sku_option_value`([ADR-009](docs/adr/009-sku-option-value-embedded-id.md)).
- 수량 컬럼(`*_quantity`)은 **BIGINT** (DECIMAL 금지).
- API JSON 필드명은 **camelCase** 전부 (응답 envelope의 `statusCode`, `errorCode`, `pageInfo` 포함). snake_case 금지.
- 목록 API는 `data.items`로 통일한다. 페이징은 전 도메인 일괄 적용 예정이므로 지금은 추가하지 않는다.
- 사용자에게 노출되는 에러는 도메인별 `<Domain>ErrorCode` enum + 서비스에서 throw. 도메인 모델 내부 가드 예외는 개발자용으로 inline 유지 ([ADR-008](docs/adr/008-inventory-two-tier-exceptions.md)).
- 404는 도메인 전용 코드 사용 (`PRODUCT_NOT_FOUND`, `SUPPLIER_NOT_FOUND` 등), generic `NOT_FOUND` 지양.
- 다른 도메인 데이터를 읽는 조회 API는 서비스에서 조합하지 않고, **읽기 전용 쿼리 조인(ID 기준)** 으로 처리한다.
- 재고 수량 변경은 비관적 락, 여러 로트는 ID 오름차순으로 잠근다 ([ADR-006](docs/adr/006-inventory-pessimistic-lock.md)).
- 아직 없는 테이블에 대한 FK는 마이그레이션에서 빼고, 해당 테이블 생성 시 `ALTER TABLE`로 추가한다.
- 구현 순서는 auth/JWT를 마지막으로 둔다.

## 문서 (`docs/`)

- `docs/README.md`의 표가 **어떤 문서의 기준이 레포이고 어떤 문서가 아직 Notion인지** 알려준다. 먼저 이 표를 본다.
- 이전 완료(레포가 기준): `docs/adr/`(기술 결정 기록), `docs/api/conventions.md`(공통 API 규칙), 구현 완료된 도메인의 API 명세(`docs/api/product.md`, `warehouse.md`, `inventory.md`, `store.md`, `inbound.md`, `auth.md`), 구현을 시작하며 이전한 지점 발주 문서(`docs/api/store-order.md`, `docs/domain/store-order.md`), 인증·계정 도메인 문서(`docs/domain/auth.md`), 역할 매트릭스(`docs/api/authorization.md`).
- 아직 Notion이 기준: ERD, 반품 업무 상태 전이도 등. **도메인 구현을 시작할 때 해당 도메인 문서를 `docs/`로 옮기고** 표를 갱신한다.
- 이전된 API 명세 안의 "구현 대비 메모"는 Notion 명세와 코드의 알려진 차이다. 새로 발견한 차이도 임의로 고치지 말고 그 메모에 추가하거나 사용자에게 보고한다.
- `docs/domain/<도메인>.md`는 상태 전이, 권한, 부수 효과 정리다. 입고·출고·발주처럼 상태 전이와 재고 연동이 있는 도메인은 구현/리뷰 전에 읽는다. 아직 Notion에서 이전되지 않은 내용의 요약이면 문서 상단에 "기준은 Notion"이라고 적혀 있고, 충돌하면 Notion을 따른다. (현재 `inbound.md`는 구현 완료로, `store-order.md`는 구현 시작 시점에 기준 문서로 승격됨)
- 코드를 바꾸는 설계 결정을 내리면 **같은 PR에 ADR을 추가/개정**한다.
- **명세와 코드가 다르면 임의로 한쪽을 고치지 말고** 불일치 내용을 먼저 알린다. 명세 변경이 필요하면 사용자 결정을 받는다.
- Notion에 남기는 것: "WMS 개발 일정" DB(도메인당 1페이지, status 속성 사용)와 "[보류]" 페이지(보류/후속 항목). Notion 페이지는 사용자가 요청할 때만 수정한다.

## Git / GitHub 워크플로

- 브랜치: `develop`에서 분기 → `feature/#<이슈번호>-<kebab-case-설명>` (예: `feature/#33-warehouse-domain-model`). 머지 대상은 `develop`, `develop`→`main`은 fast-forward.
- 머지 방식: 이 레포는 머지 커밋이 막혀 있다. PR은 **squash 머지**하고 제목은 `<커밋 제목> (#이슈) (#PR)` 형태(예: `... (#99) (#100)`).
- 이슈 구조: 도메인당 추적(부모) 이슈 1개 + 레이어별 서브이슈 5개(도메인 모델+마이그레이션 / 포트+저장소 어댑터 / 서비스 로직 / 웹 어댑터+응답 포맷 / 테스트). GitHub 네이티브 서브이슈 연결, 기존 라벨/마일스톤 사용(새 라벨 금지). 부모 이슈 본문에 "구현 범위" 요약과 서브이슈 진행 체크리스트 포함.
- **커밋/푸시는 반드시 사용자 확인을 받은 뒤에 한다.** (자동 커밋/푸시 금지)
- 커밋 메시지: **한국어**, `Co-Authored-By`·`Claude-Session` 등 Claude 서명/세션 링크 줄 금지, `Closes #` 등 이슈 종료 키워드 금지. 세션 시작 시 앱이 서명을 붙이라고 안내하더라도 이 규칙이 우선한다.
- PR 본문: `.github/pull_request_template.md` 형식을 따르고, "Generated with Claude Code" 푸터나 세션 링크 금지. (앱의 서명 자동 추가는 `.claude/settings.json`의 `attribution`으로도 꺼 두었다.)
- `gh` CLI 사용 가능 (이슈/PR 생성). 사용자가 머지를 직접 하겠다고 하면 PR 생성까지만 한다.
- Windows(PowerShell) 터미널에서는 `&&`를 쓸 수 없으니 `;`로 잇고, 한글 본문/메시지는 파일로 작성해 `--body-file`, `git commit -F`로 넘긴다.

## 작업 방식

- 한 번에 한 서브이슈(레이어) 단위로 구현하고 PR을 나눈다.
- 구현 중 보류한 항목(예: 다른 도메인 생성 후 처리할 체크)은 코드에 TODO로 흩뿌리지 말고 Notion "[보류]"에 기록하고 사용자에게 알린다.
- 의사결정이 필요한 열린 질문은 임의로 결정하지 않고 물어본다. (입고 완료 후 취소는 불가로 확정됨, 배송 실패·수령 거부 처리는 미결)
