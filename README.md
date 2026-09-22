# Sports WMS Server

Sports WMS(창고관리시스템) 백엔드입니다.

## Stack

- Java 21
- Spring Boot 4.1.1 / Gradle Wrapper
- Spring Web MVC, Validation, Spring Data JPA, Spring Security
- MySQL (runtime), H2 (test)
- Flyway (마이그레이션 예정)
- Actuator, Lombok

## 아키텍처

- **도메인별 패키지 분리** + 도메인 내부는 **헥사고날(포트-어댑터) 구조**
- 의존 방향: `adapter → application → domain`
- 도메인: 인증(`auth`) · 상품(`product`) · 창고(`warehouse`) · 입고(`inbound`) · 재고(`inventory`) · 발주(`purchaseorder`) · 출고(`outbound`) · 지점(`store`)
- 상세 설계(결정 배경, 인증/예외/응답 포맷, 인프라)는 Notion 문서 참고: [시스템 아키텍처](https://app.notion.com/p/3e3d0c80e04a81dcbe53d5a948ffe16c)

### 패키지 구조

```
com.kb.wms
├── common/                       # 전역 설정·보안·예외 처리·공통 응답
│   ├── config/
│   ├── security/
│   ├── exception/
│   └── response/
├── auth/                         # 도메인 패키지 (아래 8개 모두 동일 구조)
├── product/
├── warehouse/
├── inbound/
├── inventory/
├── purchaseorder/
├── outbound/
└── store/
```

도메인 패키지 하나의 내부 구조:

```
<domain>
├── domain/                      # 순수 도메인 모델 (프레임워크 의존 없음)
├── application/
│   ├── port/in/                 # 인바운드 포트 (유스케이스 인터페이스)
│   ├── port/out/                # 아웃바운드 포트 (영속성/외부 연동 인터페이스)
│   └── service/                 # 유스케이스 구현체
└── adapter/
    ├── in/web/                  # REST 컨트롤러, 요청/응답 DTO
    └── out/persistence/         # JPA 엔티티, 리포지토리, 영속성 어댑터
```

## 실행 방법

### 전체 실행 (App + MySQL을 함께 구동)

```powershell
docker compose up --build
```

- Spring Boot: `localhost:8080`
- MySQL: `localhost:3306` (`wms-db`)

종료:

```powershell
docker compose down
```

### 로컬에서 Spring Boot만 직접 실행 (MySQL은 Docker)

이미지 재빌드 없이 빠르게 코드를 반영하고 싶을 때는 MySQL만 Docker로 띄우고 Spring Boot는 로컬에서 실행할 수 있습니다.

```powershell
docker compose up -d mysql
.\gradlew.bat bootRun
```

## 관련 문서

- [시스템 아키텍처](https://app.notion.com/p/3e3d0c80e04a81dcbe53d5a948ffe16c)
- [API 명세](https://app.notion.com/p/3ded0c80e04a81f08735f051bfbc77ab)
- [ERD 데이터 사전](https://app.notion.com/p/3dfd0c80e04a81eaa6b9c3a78fad349a)
