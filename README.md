# Sports WMS Server

Sports WMS 백엔드입니다.

## Stack

- Java 21
- Spring Boot 4.1.1 / Gradle Wrapper
- Spring Web MVC, Validation, Spring Data JPA, Spring Security
- MySQL (runtime), H2 (test)
- Actuator, Lombok

## Run

```powershell
.\gradlew.bat bootRun
```

## Run with MySQL

개발 중에는 MySQL만 Docker로 실행하고 Spring Boot는 로컬에서 실행합니다. Docker 이미지 빌드 없이 빠르게 코드 변경을 반영할 수 있습니다.

```powershell
docker compose up -d mysql
```

별도 터미널에서 Spring Boot를 실행합니다.

```powershell
.\gradlew.bat bootRun
```

- MySQL: `localhost:3306` (`wms-db`)
- Spring Boot: `localhost:8080` (로컬 실행)

종료하려면 다음을 실행합니다.

```powershell
docker compose down
```

Docker로 Spring Boot까지 통합 실행해야 할 때는 `Dockerfile`을 사용해 별도 Compose 서비스를 추가할 수 있습니다.
