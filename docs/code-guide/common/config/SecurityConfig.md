# SecurityConfig

`common/config/SecurityConfig.java` · `@Configuration`

Spring Security 설정. 엔드포인트별 **역할 규칙**이 한곳에 모여 있다(`docs/api/authorization.md` 표와 맞춘다).

## 빈

| 빈 | 설명 |
|---|---|
| `passwordEncoder()` | `BCryptPasswordEncoder`. 가입·로그인·비밀번호 변경이 사용한다 |
| `filterChain(http, jwtProvider, userResolver, exceptionHandler)` | 보안 필터 체인 |

## 필터 체인 구성

| 설정 | 내용 |
|---|---|
| CSRF·httpBasic·formLogin | 모두 비활성화 |
| 세션 | `STATELESS` |
| 예외 처리 | 인증 실패(`authenticationEntryPoint`)와 권한 없음(`accessDeniedHandler`) 모두 `RestSecurityExceptionHandler` |
| JWT 필터 | `JwtAuthenticationFilter`를 `UsernamePasswordAuthenticationFilter` 앞에 직접 추가 |

## 경로 규칙

규칙은 **위에서부터 먼저 맞는 것이 적용**되므로 구체적인 경로가 위에 있다. 담당 창고·지점 범위와 작성자 같은 **데이터 단위 검사는 서비스**가 한다(ADR-012).

| 영역 | 경로 | 허용 |
|---|---|---|
| 공개 | POST `/auth/signup`, `/auth/login`, Swagger(`/v3/api-docs/**`, `/swagger-ui/**`), `/actuator/health/**` | 모두 |
| 내 정보 | `/auth/me`, `/auth/me/password` | 인증만 |
| 사용자 | `/users/**` | HQ_ADMIN |
| 상품 | GET `/products/**` | 인증된 모든 역할 |
| 상품 | 그 외 `/products/**` | HQ_ADMIN |
| 창고 | GET `/warehouses/management-types`, `/section-types` | 인증만 |
| 창고 | GET `/warehouses/my` | WAREHOUSE_MANAGER |
| 창고 | GET `/warehouses`, `/managers`, `/sections` | HQ_ADMIN |
| 창고 | GET `/warehouses/*`, `/*/sections`, `/sections/*` | HQ_ADMIN, WAREHOUSE_MANAGER |
| 창고 | 그 외 `/warehouses/**` | HQ_ADMIN |
| 재고 | POST `/inventory/adjustments` | WAREHOUSE_MANAGER |
| 로트 | PATCH `/lots/*/status` | HQ_ADMIN (여러 창고가 공유하는 마스터라서) |
| 재고·로트 | 그 외 `/inventory/**`, `/lots/**` | HQ_ADMIN, WAREHOUSE_MANAGER (점주 불가) |
| 지점 | GET `/stores/management-types` | 인증만 |
| 지점 | GET `/stores/my` | STORE_OWNER |
| 지점 | GET `/stores`, `/stores/managers` | HQ_ADMIN |
| 지점 | GET `/stores/*` | HQ_ADMIN, STORE_OWNER |
| 지점 | 그 외 `/stores/**` | HQ_ADMIN |
| 공급처 | GET `/suppliers`, `/suppliers/*` | HQ_ADMIN, WAREHOUSE_MANAGER |
| 공급처 | 그 외 `/suppliers/**` | HQ_ADMIN |
| 창고 발주 | PATCH `/purchase-orders/*/confirm` | HQ_ADMIN |
| 창고 발주 | PATCH `/purchase-orders/*/cancel` | HQ_ADMIN, WAREHOUSE_MANAGER |
| 창고 발주 | POST `/purchase-orders` | WAREHOUSE_MANAGER |
| 창고 발주·입고 | GET `/purchase-orders/**`, `/inbounds/**` | HQ_ADMIN, WAREHOUSE_MANAGER |
| 입고 | 그 외 `/inbounds/**` | WAREHOUSE_MANAGER |
| 지점 발주 | POST `/orders` | STORE_OWNER |
| 지점 발주 | GET `/orders` | HQ_ADMIN |
| 지점 발주 | GET `/orders/my` | STORE_OWNER, WAREHOUSE_MANAGER |
| 지점 발주 | GET `/orders/*`, `/orders/*/details` | HQ_ADMIN, STORE_OWNER, WAREHOUSE_MANAGER |
| 지점 발주 | PATCH `/orders/*/cancel` | HQ_ADMIN, STORE_OWNER |
| 지점 발주 | PATCH `/orders/*/hold`, `/resume`, `/complete-partial` | WAREHOUSE_MANAGER |
| 지점 발주 | 그 외 `/orders/**` (승인·반려·배정 등) | HQ_ADMIN |
| 재고 할당·출고 | GET `/allocations/**`, `/outbounds/**` | HQ_ADMIN, WAREHOUSE_MANAGER |
| 재고 할당·출고 | 그 외 `/allocations/**`, `/outbounds/**` | WAREHOUSE_MANAGER |
| 나머지 | 모든 요청 | 인증만 |

## 주의

- 역할 이름은 `hasRole("X")`로 쓰며, 필터가 권한을 `ROLE_` + `UserRole.name()`으로 올려 맞춘다.
- 경로 규칙이 허용해도 서비스의 범위 검사(`AuthenticatedUser.requireWarehouseAccess` 등)에서 403이 날 수 있다.
