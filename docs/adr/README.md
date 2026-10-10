# ADR (기술 결정 기록)

주요 기술 선택과 설계 결정, 대안 검토 및 선택 근거를 기록한다. 이 디렉터리가 기준(source of truth)이며, 2026-10-02에 Notion "ADR / 기술 결정 기록"에서 이전했다.

## 작성 규칙
- 파일명: `NNN-kebab-case-제목.md` (번호는 이어서 부여, 번호 재사용 금지)
- 구성: 상태 / 컨텍스트 / 결정 / 근거 / 검토했던 대안 / 영향
- 상태 값: `Accepted`, `Superseded`(다른 ADR로 대체), `Deprecated`. 결정을 뒤집을 때는 기존 ADR을 지우지 않고 새 ADR을 쓰거나 개정 이력을 남긴다 (예: ADR-005 개정).
- 코드를 바꾸는 결정을 내렸다면 같은 PR에 ADR을 포함한다.

## 목록

| 번호 | 제목 | 상태 | 날짜 |
|---|---|---|---|
| [001](001-inventory-query-api-split.md) | 재고 조회 API를 SKU 집계·로트 상세·Lot 마스터로 분리 | Accepted | 2026-09-21 |
| [002](002-purchase-order-confirm-by-hq.md) | PurchaseOrder 확정(CONFIRMED) 권한을 본사 관리자로 지정 | Accepted | 2026-09-21 |
| [003](003-master-data-owned-by-hq.md) | 마스터 데이터 관리 주체를 본사 관리자로 일원화 | Accepted | 2026-09-21 |
| [004](004-lot-find-or-create-on-inspect.md) | Lot은 별도 생성 API 없이 입고 검수 트랜잭션 내 find-or-create로 생성 | Accepted | 2026-09-21 |
| [005](005-entity-reference-by-id.md) | 엔티티 참조는 도메인 구분 없이 전부 ID 참조 (Long xxxId) | Accepted (개정) | 2026-09-22 |
| [006](006-inventory-pessimistic-lock.md) | 재고 수량 갱신에 비관적 락(PESSIMISTIC_WRITE) 사용 | Accepted | 2026-09-24 |
| [007](007-inventory-query-jpql-cross-domain-join.md) | 재고 조회 API는 도메인 간 테이블을 JPQL로 직접 조인 | Accepted | 2026-09-24 |
| [008](008-inventory-two-tier-exceptions.md) | 재고 도메인은 가드 예외와 사용자 응답 오류(ErrorCode)를 2단으로 분리 | Accepted | 2026-09-24 |
| [009](009-sku-option-value-embedded-id.md) | sku_option_value 복합키는 @Embeddable/@EmbeddedId로 구현 | Accepted | 2026-09-24 |
| [010](010-store-order-progress-stage-derived.md) | 지점 발주 진행 단계(progressStage)는 저장하지 않고 파생 값으로 계산 | Accepted | 2026-10-04 |
| [011](011-jwt-access-token-only.md) | 액세스 토큰만 발급하는 JWT 인증 (HS256, jjwt) | Accepted | 2026-10-06 |
| [012](012-authorization-check-placement.md) | 인가 검사 위치 — 역할은 보안 설정, 소속·작성자는 서비스 | Accepted | 2026-10-08 |
| [013](013-initial-hq-admin-from-environment.md) | 최초 본사 관리자는 시작 시 환경변수로 생성 | Accepted | 2026-10-08 |
| [014](014-reactivation-and-sku-deactivation-guard.md) | 재활성화 API는 상위 확인 후 허용하고, SKU 비활성화에도 사용 중 가드를 둔다 | Accepted | 2026-10-10 |
| [015](015-status-history-own-package.md) | 상태 이력은 `common`이 아닌 독립 도메인 패키지(`statushistory`)로 둔다 | Accepted | 2026-10-10 |
| [016](016-reload-user-per-request.md) | 토큰은 사용자 ID만 담고, 역할·상태·소속은 요청마다 DB에서 읽는다 | Accepted | 2026-10-10 |
| [017](017-sku-price-required-and-update.md) | SKU 단가를 필수로 하고, SKU 수정 API와 단가 변경 이력을 둔다 | Accepted | 2026-10-10 |
| [018](018-warehouse-capacity-required-and-hierarchy.md) | 창고·구역 수용량을 필수로 하고, 상하위 수용량 관계를 검증한다 | Accepted | 2026-10-10 |
