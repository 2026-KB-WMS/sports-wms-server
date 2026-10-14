# StoreCodeService

`store/application/service/StoreCodeService.java` · 구현: `StoreCodeUseCase` · `@Transactional(readOnly = true)`

지점 도메인 고정 코드 목록 조회. DB 조회 없이 enum 값을 반환한다.

## 메서드

| 메서드 | 설명 |
|---|---|
| `getManagementTypes()` | `StoreManagementType` 전체를 `CodeItem(code, description)`으로 반환 (OWNER/점주, MANAGER/지점 관리자) |
