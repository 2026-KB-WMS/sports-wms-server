# 설정 클래스 (config)

`common/config/` · `SecurityConfig`는 별도 문서(`SecurityConfig.md`)를 본다.

## JpaAuditingConfig

| 항목 | 내용 |
|---|---|
| 용도 | JPA Auditing(`@CreatedDate`, `@LastModifiedDate`)을 켠다 |
| 어노테이션 | `@Configuration`, `@EnableJpaAuditing` |
| 사용처 | `BaseTimeEntity`의 `createdAt`, `updatedAt` 자동 채움 |
| 왜 분리했나 | `@EnableJpaAuditing`을 메인 클래스에 두면 `@WebMvcTest` 같은 슬라이스 테스트에서 JPA 메타모델이 비어 `jpaMappingContext` 빈 생성이 실패한다 |

## OpenApiConfig

| 항목 | 내용 |
|---|---|
| 용도 | Swagger / OpenAPI 문서 설정 |
| 접속 | `/swagger-ui/index.html` |
| 빈 | `openApi()`: 제목 "WMS API", 설명 "Sports WMS 백엔드 API 문서", 버전 `v1` |
| 인증 | HTTP Bearer(JWT) 스킴 `bearerAuth`를 전역 보안 항목으로 등록 → Swagger에서 토큰을 넣고 호출 가능 |
