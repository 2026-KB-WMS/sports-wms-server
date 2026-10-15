# SkuSupplierService

`inbound/application/service/SkuSupplierService.java` · 구현: `SkuSupplierUseCase` · 클래스 기본 `@Transactional(readOnly = true)`

SKU↔공급처 다대다 매핑(ADR-029) 조회·등록·삭제. 매핑은 발주 확정 때 라인에 지정할 수 있는 공급처 후보를 정한다.

## 의존

`SkuSupplierRepository`, `SupplierRepository`, `SkuLookupPort`(상품 도메인 SKU 존재 확인)

## 메서드

| 메서드 | 트랜잭션 | 설명 |
|---|---|---|
| `getSuppliers(skuId)` | 읽기 | 매핑 등록 순. SKU가 없으면 상품 도메인의 `SKU_NOT_FOUND` |
| `registerSupplier(skuId, supplierId)` | 쓰기 | 공급처 없음 → `SUPPLIER_NOT_FOUND`, 비활성 → 409 `SUPPLIER_INACTIVE`(이미 걸린 매핑은 유지), 이미 있음 → 409 `SKU_SUPPLIER_ALREADY_MAPPED` |
| `removeSupplier(skuId, supplierId)` | 쓰기 | 매핑이 없으면 404 `SKU_SUPPLIER_NOT_FOUND`. 이미 확정된 발주에는 영향이 없다 |
