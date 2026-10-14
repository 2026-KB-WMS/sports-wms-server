# statushistory 코드 가이드

`src/main/java/com/kb/wms/statushistory/`

## 도메인 개요

**업무 엔티티의 상태 전이를 한 곳에 기록하는 공용 이력 도메인**이다. 사용자·발주·입고·지점 발주·재고 할당·출고·지점·로트가 상태를 바꿀 때 이 도메인의 `record`를 호출한다. 컨트롤러가 없고 다른 도메인의 서비스가 호출한다.

## 문서

| 문서 | 내용 |
|---|---|
| `domain/StatusHistory.md` | 이력 엔티티, 엔티티 유형 enum, 기록 주체 표 |
| `application/StatusHistoryApplication.md` | UseCase·Service·조회 뷰·Repository 포트 |
| `adapter/out/persistence/Persistence.md` | 저장 어댑터, JPA 리포지토리·엔티티 |

## 핵심 규칙

- **같은 트랜잭션에서만 기록**한다(`Propagation.MANDATORY`). 상태 변경이 롤백되면 이력도 롤백된다
- **추가 전용**이다. 한 번 기록한 이력은 수정하지 않는다
- 이전·이후 상태는 **문자열**이다(엔티티마다 상태 enum이 달라서)
- 처리자(`changedBy`)는 **필수**다. 시스템 자동 전이는 트리거한 사용자를 넣는다
- 사유는 **500자 이하**, 빈 값은 null
- **취소·반려·보류 사유**는 별도 컬럼이 아니라 이 이력에서 `findStatusReason`으로 읽는다
- 최초 생성 기록은 `fromStatus = null`
