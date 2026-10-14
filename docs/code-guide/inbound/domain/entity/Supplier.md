# Supplier

`inbound/domain/entity/Supplier.java`

창고 발주(`PurchaseOrder`)의 대상이 되는 **공급업체 마스터**. 삭제 없이 비활성화(ACTIVE → INACTIVE)만 허용한다.

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| supplierId | Long | PK |
| supplierCode | String | 공급처 코드 (유니크). **직접 입력**(창고·지점과 달리 서버 발급이 아니다) |
| name | String | 공급처명 |
| managerName | String | 담당자명 (필수) |
| contactNumber | String | 연락처 (필수) |
| email | String | 이메일 (선택) |
| address | String | 주소 (선택) |
| latitude, longitude | BigDecimal | 위도·경도. 소수 7자리로 반올림 |
| status | SupplierStatus | 생성 시 null이면 ACTIVE |
| createdAt, updatedAt | LocalDateTime | |

## 메서드

| 메서드 | 설명 |
|---|---|
| `register(code, name, managerName, contactNumber, email, address)` | ACTIVE로 생성 |
| `changeName`, `changeManagerName`, `changeContactNumber`, `changeEmail`, `changeAddress` | 필드 변경. email·address는 null로 비울 수 있다 |
| `changeCoordinates(lat, lon)` | 쌍으로 변경 |
| `deactivate()` | 이미 비활성이면 `IllegalStateException` |
| `activate()` | 이미 활성이면 `IllegalStateException` |
| `isActive()` | ACTIVE 여부 |

`supplierCode`는 등록 후 수정할 수 없어 변경 메서드가 없다.
