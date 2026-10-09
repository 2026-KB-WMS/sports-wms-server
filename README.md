# Sports WMS Server

Sports WMS(창고관리시스템) 백엔드입니다. 본사·창고·지점이 함께 쓰는 상품, 입고, 재고, 지점 발주, 출고 업무를 다룹니다.

## Stack

- Java 21
- Spring Boot 4.1.1 / Gradle Wrapper
- Spring Web MVC, Validation, Spring Data JPA, Spring Security (JWT 액세스 토큰)
- MySQL 8.4 (runtime), H2 (test)
- Flyway (`src/main/resources/db/migration`)
- Actuator, Lombok

## 도메인

| 패키지 | 설명 |
|---|---|
| `auth` | 가입, 로그인, 내 정보, 계정 관리, 비밀번호 변경 |
| `product` | 상품, SKU, 옵션 |
| `warehouse` | 창고, 구역 |
| `inbound` | 공급처, 창고 발주(PurchaseOrder), 입고 |
| `inventory` | 재고, 로트 |
| `storeorder` | 지점 발주 (지점 → 창고 요청) |
| `outbound` | 출고, 재고 할당 |
| `store` | 지점 |

반품은 MVP 범위 밖입니다.

## 아키텍처

- **도메인별 패키지 분리** + 도메인 내부는 **헥사고날(포트-어댑터) 구조**
- 의존 방향: `adapter → application → domain`
- 엔티티 간 참조는 JPA 연관관계 대신 `Long xxxId`만 사용합니다. ([ADR-005](docs/adr/005-entity-reference-by-id.md))
- 설계 결정의 배경은 [`docs/adr/`](docs/adr/README.md)에 있습니다.

```
com.kb.wms
├── common/                       # 전역 설정·보안·예외 처리·공통 응답
└── <domain>/                     # 위 8개 도메인, 모두 동일 구조
```

도메인 패키지 하나의 내부 구조:

```
<domain>
├── domain/
│   ├── entity/                  # 엔티티
│   └── enums/                   # 상태 enum
├── application/
│   ├── port/in/                 # 인바운드 포트 (유스케이스 인터페이스)
│   ├── port/out/                # 아웃바운드 포트 (영속성 인터페이스)
│   └── service/                 # 유스케이스 구현체
├── adapter/
│   ├── in/web/                  # REST 컨트롤러, 요청/응답 DTO
│   └── out/persistence/         # JPA 엔티티, 리포지토리, 영속성 어댑터
└── exception/                   # <Domain>ErrorCode
```

## 실행 방법

### 1. 환경 변수

`.env.example`을 `.env`로 복사해 값을 채웁니다.

```powershell
Copy-Item .env.example .env
```

| 변수 | 용도 |
|---|---|
| `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD` | MySQL 컨테이너 설정 |
| `WMS_ADMIN_LOGIN_ID`, `WMS_ADMIN_PASSWORD`, `WMS_ADMIN_NAME`, `WMS_ADMIN_EMAIL`, `WMS_ADMIN_PHONE` | 최초 본사 관리자 계정 ([ADR-013](docs/adr/013-initial-hq-admin-from-environment.md)) |

본사 관리자가 하나도 없을 때만 시작 시 위 값으로 첫 관리자를 만듭니다. 모두 비워 두면 만들지 않습니다.

### 2. 전체 실행 (App + MySQL을 함께 구동)

```powershell
docker compose up --build
```

- Spring Boot: `localhost:8080`
- MySQL: `localhost:3306` (`wms-db`)

종료:

```powershell
docker compose down
```

### 3. 로컬에서 Spring Boot만 직접 실행 (MySQL은 Docker)

이미지 재빌드 없이 빠르게 코드를 반영하고 싶을 때는 MySQL만 Docker로 띄우고 Spring Boot는 로컬에서 실행할 수 있습니다.

```powershell
docker compose up -d mysql
.\gradlew.bat bootRun
```

로컬 실행 시 기본 접속 정보는 `application.yml`의 `jdbc:mysql://localhost:3306/wms` (`wms` / `wms`)입니다. `.env`의 `MYSQL_*` 값을 이 기본값과 다르게 썼다면 `SPRING_DATASOURCE_*` 환경 변수로 맞춰 주세요. JWT 서명 키도 로컬 개발용 기본값이 들어 있습니다.

`prod` 프로파일은 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`을 반드시 환경 변수로 받습니다.

## API 사용 요약

Base path는 `/api/v1`입니다. 전체 규칙은 [공통 API 규칙](docs/api/conventions.md)을 참고하세요.

### 인증

1. `POST /api/v1/auth/login`에 `loginId`, `password`를 보내 액세스 토큰(JWT)을 받습니다.
2. 이후 요청에 `Authorization: Bearer {accessToken}` 헤더를 붙입니다. 가입·로그인만 헤더 없이 호출합니다.
3. 토큰 만료는 3600초이며 리프레시 토큰은 없습니다. 만료되면 다시 로그인합니다. ([ADR-011](docs/adr/011-jwt-access-token-only.md))

### 응답 형식

JSON 필드명은 모두 camelCase입니다.

성공:

```json
{
  "success": true,
  "statusCode": 200,
  "message": "요청에 성공하였습니다.",
  "data": { }
}
```

- 목록 조회는 `data.items` 배열로 내려갑니다. 페이징(`pageInfo`)은 아직 없습니다.
- 생성은 `statusCode`가 `201`입니다.

실패:

```json
{
  "success": false,
  "statusCode": 400,
  "message": "입력값을 확인해주세요.",
  "errorCode": "VALIDATION_ERROR",
  "errors": [{ "field": "quantity", "reason": "1 이상이어야 합니다" }]
}
```

- 도메인별 오류는 `PRODUCT_NOT_FOUND`처럼 도메인 전용 `errorCode`를 씁니다.

## 빌드 / 테스트

```powershell
.\gradlew.bat build
.\gradlew.bat test
```

테스트는 H2를 사용하므로 MySQL이 없어도 실행됩니다.

## 문서

명세와 결정 문서는 레포의 [`docs/`](docs/README.md)가 기준입니다. 어떤 문서가 레포 기준이고 어떤 문서가 아직 Notion에 있는지는 [`docs/README.md`](docs/README.md)의 표를 먼저 확인하세요.

- [ADR (기술 결정 기록)](docs/adr/README.md)
- [공통 API 규칙](docs/api/conventions.md)
- [인가 규칙·역할 매트릭스](docs/api/authorization.md)
- 도메인별 API 명세: [auth](docs/api/auth.md) · [product](docs/api/product.md) · [warehouse](docs/api/warehouse.md) · [inventory](docs/api/inventory.md) · [store](docs/api/store.md) · [inbound](docs/api/inbound.md) · [store-order](docs/api/store-order.md) · [outbound](docs/api/outbound.md)
- 도메인 상태 전이·권한·부수 효과: [auth](docs/domain/auth.md) · [inbound](docs/domain/inbound.md) · [store-order](docs/domain/store-order.md) · [outbound](docs/domain/outbound.md)

아직 Notion에 있는 문서:

- [시스템 아키텍처](https://app.notion.com/p/3e3d0c80e04a81dcbe53d5a948ffe16c)
- [API 명세](https://app.notion.com/p/3ded0c80e04a81f08735f051bfbc77ab)
- [ERD 데이터 사전](https://app.notion.com/p/3dfd0c80e04a81eaa6b9c3a78fad349a) (컬럼 정의의 실제 기준은 Flyway SQL)

## 기여

- 브랜치는 `develop`에서 `feature/#<이슈번호>-<kebab-case-설명>`으로 분기합니다.
- PR은 squash 머지하며 제목은 `<커밋 제목> (#이슈) (#PR)` 형태입니다.
- 커밋 메시지는 한국어로 작성합니다.
