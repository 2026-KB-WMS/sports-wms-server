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

MySQL 연결 정보와 데이터베이스 마이그레이션은 인프라/DB 설계 단계에서 추가합니다.
