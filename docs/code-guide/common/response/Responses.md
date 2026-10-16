# 공통 응답 (response)

`common/response/` · 모든 API가 쓰는 응답 envelope. JSON 필드는 camelCase다.

## ApiResponse&lt;T&gt;

| 항목 | 내용 |
|---|---|
| 용도 | 성공 응답 공통 포맷 |
| 필드 | `success`(항상 true), `statusCode`, `message`, `data` |

| 팩토리 | statusCode | message |
|---|---|---|
| `ok(data)` | 200 | 요청에 성공하였습니다. |
| `ok(message, data)` | 200 | 직접 지정 |
| `created(data)` | 201 | 생성되었습니다. |
| `of(statusCode, message, data)` | 직접 지정 | 직접 지정 |

컨트롤러의 `@ResponseStatus(CREATED)`와 `ApiResponse.created`를 같이 써서 HTTP 상태와 본문 `statusCode`를 맞춘다.

## ErrorResponse

| 항목 | 내용 |
|---|---|
| 용도 | 오류 응답 공통 포맷 |
| 필드 | `success`(항상 false), `statusCode`, `message`, `errorCode`, `errors` |
| 내부 record | `FieldError(field, reason)`: 검증 실패한 필드와 사유 |

| 팩토리 | 설명 |
|---|---|
| `of(errorCode, message)` | `errorCode` 문자열은 공통 코드 이름, `errors`는 빈 목록 |
| `of(errorCode, errorCodeName, message)` | 도메인 코드 이름을 `errorCode` 문자열로 지정 |
| `of(errorCode, message, errors)` | 필드 오류 목록 포함(검증 실패용) |

사용처: `GlobalExceptionHandler`, `RestSecurityExceptionHandler`(필터 체인 밖 오류라 직접 씀).

## ItemsResponse&lt;T&gt;

| 항목 | 내용 |
|---|---|
| 용도 | 목록을 `data.items`로 감싼다 (프로젝트 규칙: 목록 API는 `data.items`로 통일) |
| 필드 | `items` |
| 팩토리 | `of(items)` |

페이지네이션을 적용하지 않는 목록(마스터·코드성 목록 등)에 쓴다. 적용하는 목록은 아래 `PageResponse`를 쓴다.

## PageResponse&lt;T&gt;

| 항목 | 내용 |
|---|---|
| 용도 | 페이지네이션을 적용한 목록 응답: `data.items`와 `data.pageInfo` ([ADR-032](../../../adr/032-pagination-and-sorting.md)) |
| 필드 | `items`, `pageInfo`(`page`, `size`, `totalElements`, `totalPages`, `sort`) |
| 팩토리 | `of(PageResult)`, `of(PageResult, mapper)`: 결과 항목을 응답 DTO로 바꿔 담는다 |

`sort`는 실제 적용된 첫 정렬 기준(예: `createdAt,desc`)이다. 만드는 방법은 `paging/Paging.md` 참고.
