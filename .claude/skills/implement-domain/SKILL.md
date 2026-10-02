---
name: implement-domain
description: WMS 백엔드의 도메인(또는 도메인 일부)을 명세 기반으로 구현할 때 사용. 명세 docs/ 이전 → 이슈 확인 → 브랜치 → 레이어별 구현 → 테스트 → 커밋/PR → Notion 진행 기록까지의 표준 절차.
---

# 도메인 구현 절차

대상 도메인/서브이슈를 사용자에게 확인한 뒤 아래 순서로 진행한다. 규칙 세부사항은 AGENTS.md와 `docs/adr/`를 따른다.

## 0. 명세 준비 (docs/가 기준)
1. `docs/README.md`의 기준 위치 표에서 대상 도메인 문서가 레포에 이미 있는지 확인한다.
2. 아직 Notion에만 있으면 **먼저 `docs/`로 이전한다**: 해당 도메인의 API 명세, 업무 상태 전이도(해당 도메인 부분), 필요한 ERD 부분을 Notion에서 읽어 `docs/api/<도메인>.md`, `docs/domain/<도메인>.md` 등으로 옮기고 `docs/README.md` 표를 갱신한다. 이전은 구현과 별도 커밋/PR(문서 이슈)로 분리해도 된다. 옮긴 문서 상단에 이전 날짜와 Notion 원본 수정일을 적는다. Notion 원본 페이지 수정(이전 안내 문구 추가)은 사용자 승인 후에만 한다.
3. 이미 레포에 있으면 Notion 수정일이 더 최신인지 확인하지 않는다 (기준은 레포).
4. 명세에 모호하거나 충돌하는 부분은 구현 전에 목록으로 정리해 사용자에게 묻는다.
5. 참고 구현으로 `product` 도메인의 같은 레이어 코드를 읽는다.

## 1. 이슈 / 브랜치
- 이슈가 없으면 `create-domain-issues` skill을 먼저 사용한다.
- `develop` 최신화 후 `feature/#<서브이슈번호>-<kebab-case-설명>` 생성.

## 2. 레이어별 구현 (서브이슈 1개 = 레이어 1개)
1. **도메인 모델 + 마이그레이션**: `domain/entity`, `domain/enums`, 새 Flyway 파일(`V{다음번호}__...sql`). 아직 없는 테이블 FK는 제외. 엔티티 참조는 `Long xxxId`, 수량은 BIGINT.
2. **포트 + 저장소 어댑터**: `application/port/in|out`, `adapter/out/persistence` (entity/repository 포함). 재고 수량 변경이면 비관적 락.
3. **서비스 로직**: `application/service`, `exception/<Domain>ErrorCode`. 404는 도메인 전용 코드.
4. **웹 어댑터 + 응답 포맷**: Controller, dto. camelCase, 목록은 `data.items`.
5. **테스트**: 도메인/서비스 단위 테스트 + 필요 시 웹 슬라이스 테스트.

## 3. 검증
- `./gradlew test` 통과 확인.
- `architecture-reviewer`, `spec-reviewer` subagent로 규칙/명세 불일치 점검 후 지적 사항 반영.

## 4. 설계 결정 기록
- 구현 중 코드에 영향을 주는 설계 결정을 새로 내렸다면(규칙 예외, 대안 선택 등) `docs/adr/`에 ADR을 추가하거나 기존 ADR을 개정하고 같은 PR에 포함할 것을 제안한다. 결정이 없었으면 건너뛴다.
- 명세와 다르게 구현한 부분이 있으면 명세(`docs/`)를 갱신할지 사용자에게 확인한다.

## 5. 커밋 / PR (확인 후 진행)
- 변경 파일과 커밋 메시지(한국어, Co-Authored-By/`Closes #` 없음) 초안을 보여주고 **사용자 승인 후** 커밋·푸시.
- PR은 `develop` 대상, `.github/pull_request_template.md` 형식, Claude 푸터 금지. 관련 이슈 연결. 머지는 squash이며 사용자가 직접 한다고 하면 PR 생성까지만.

## 6. Notion 기록
- "WMS 개발 일정" DB의 해당 도메인 페이지 status/진행 내용 갱신.
- 보류한 항목은 "[보류]" 페이지에 추가.
- 완료 보고에는 구현 결과, 명세와 달라진 점, 보류 항목을 짧게 포함한다.
