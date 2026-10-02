---
name: implement-domain
description: WMS 백엔드의 도메인(또는 도메인 일부)을 Notion 명세 기반으로 구현할 때 사용. 이슈 확인 → 브랜치 → 레이어별 구현 → 테스트 → 커밋/PR → Notion 진행 기록까지의 표준 절차.
---

# 도메인 구현 절차

대상 도메인/서브이슈를 사용자에게 확인한 뒤 아래 순서로 진행한다. 규칙 세부사항은 AGENTS.md를 따른다.

## 0. 사전 확인
1. 대상 도메인과 구현할 서브이슈 번호를 확인한다. 이슈가 없으면 `create-domain-issues` skill을 먼저 사용한다.
2. Notion에서 해당 도메인의 ERD 데이터 사전, 기능 명세, API 명세(필요하면 상태 전이도)를 읽는다. 명세에 모호하거나 충돌하는 부분은 구현 전에 목록으로 정리해 사용자에게 묻는다.
3. 참고 구현으로 `product` 도메인의 같은 레이어 코드를 읽는다.

## 1. 브랜치
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

## 4. 커밋 / PR (확인 후 진행)
- 변경 파일과 커밋 메시지(한국어, Co-Authored-By/`Closes #` 없음) 초안을 보여주고 **사용자 승인 후** 커밋·푸시.
- PR은 `develop` 대상, `.github/pull_request_template.md` 형식, Claude 푸터 금지. 관련 이슈 연결.

## 5. Notion 기록
- "WMS 개발 일정" DB의 해당 도메인 페이지 status/진행 내용 갱신.
- 보류한 항목은 "[보류]" 페이지에 추가.
- 완료 보고에는 구현 결과, 명세와 달라진 점, 보류 항목을 짧게 포함한다.
