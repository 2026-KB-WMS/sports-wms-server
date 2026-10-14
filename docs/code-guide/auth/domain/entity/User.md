# User

`auth/domain/entity/User.java`

로그인·권한·계정 상태를 관리하는 **회원**. 가입 신청 계정은 PENDING으로 만들어지고 본사 관리자가 승인(ACTIVE)해야 로그인할 수 있다.

**창고·지점 소속은 이 엔티티에 두지 않는다.** `WarehouseMember`, `StoreMember`가 가진다. 입력 형식 검증(아이디·비밀번호 규칙)과 소속 배정 여부 검사는 서비스의 책임이고, 여기서는 상태 전이 같은 **도메인 불변식**만 지킨다.

## 상태 전이

```
PENDING ──approve()──▶ ACTIVE ◀──reactivate()── INACTIVE
   │                      └────deactivate()────▶    ▲
   └──────── reject() ───────────────────────────────┘
```

| 전이 | 메서드 | 의미 |
|---|---|---|
| PENDING → ACTIVE | `approve()` | 가입 승인 |
| PENDING → INACTIVE | `reject()` | 가입 반려 |
| ACTIVE → INACTIVE | `deactivate()` | 비활성화 |
| INACTIVE → ACTIVE | `reactivate()` | 재활성화 |
| (되돌리기) → PENDING | 없음 | **불가** |

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| userId | Long | PK |
| loginId | String | 로그인 아이디. **소문자로 통일해 저장**, 가입 후 수정 불가 |
| passwordHash | String | BCrypt 해시 |
| name, email, phone | String | 이름, 이메일(유니크), 연락처 |
| role | UserRole | HQ_ADMIN / WAREHOUSE_MANAGER / STORE_OWNER |
| status | UserStatus | 생성 시 null이면 PENDING |
| lastLoginAt | LocalDateTime | 마지막 로그인 시각 |
| createdAt, updatedAt | LocalDateTime | |

## 메서드

| 메서드 | 설명 | 위반 시 |
|---|---|---|
| `signUp(loginId, passwordHash, name, email, phone, role)` | **PENDING**으로 생성. HQ_ADMIN은 가입으로 만들 수 없다 | `IllegalArgumentException` |
| `createHqAdmin(...)` | 최초 본사 관리자. 승인 없이 **ACTIVE**로 생성 | `IllegalArgumentException` |
| `changePasswordHash`, `changeName`, `changeEmail`, `changePhone`, `changeRole` | 필드 변경. 빈 값 불가 | `IllegalArgumentException` |
| `approve`, `reject`, `deactivate`, `reactivate` | 위 표의 전이 | 상태가 맞지 않으면 `IllegalStateException` |
| `changeStatus(target)` | 목표 상태로 전이. ACTIVE는 PENDING이면 승인, 아니면 재활성화. INACTIVE는 PENDING이면 반려, 아니면 비활성화. PENDING으로는 불가 | `IllegalStateException` |
| `canChangeStatusTo(target)` | `changeStatus`가 성공하는지(같은 상태·PENDING 불가) | |
| `recordLogin(at)` | 로그인 시각 기록 | |
| `isActive()`, `isHqAdmin()` | 확인 | |

`loginId`는 변경 메서드가 없다.
