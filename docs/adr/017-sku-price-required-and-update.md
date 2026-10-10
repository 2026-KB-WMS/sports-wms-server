# ADR-017: SKU 단가를 필수로 하고, SKU 수정 API와 단가 변경 이력을 둔다

- 상태: Accepted
- 날짜: 2026-10-10

## 컨텍스트
SKU 등록 때 매입·공급 단가를 비울 수 있었고(DB도 NULL 허용), 등록 후 단가를 바꾸는 API가 없었다. 단가가 빈 SKU는 창고 발주(`PURCHASE_PRICE_MISSING`)와 지점 발주(`SUPPLY_PRICE_MISSING`)가 막히고, 고치려면 DB를 직접 수정하거나 SKU를 새 코드로 다시 등록해야 했다. 보류 목록의 "SKU 수정"도 열려 있었다(`docs/domain/pricing.md` 알려진 공백 1·5번).

## 결정
- **단가 필수**: `POST /products/skus`의 `currentPurchasePrice`, `currentSupplyPrice`는 필수다(없으면 400 `VALIDATION_ERROR`). 값은 0 이상이며 0도 허용한다. 도메인(`ProductSku.register`)도 같은 규칙을 가드한다.
- **DB도 NOT NULL**: V16 마이그레이션이 `product_sku.current_purchase_price`, `current_supply_price`를 `NOT NULL`로 바꾼다. 기존에 NULL이던 값은 **0으로 채운 뒤** 바꾼다.
- **SKU 수정 API**: `PATCH /products/skus/{skuId}`(HQ_ADMIN). 값이 있는 필드만 수정하며 최소 1개가 필요하다. 수정 가능: `skuName`, `barcode`, `weight`, `currentPurchasePrice`, `currentSupplyPrice`, `safetyStockQuantity`. `skuCode`, `productId`, `unit`, `isActive`를 보내면 400이다(상태는 `/status`). 바코드가 다른 SKU와 겹치면 409 `DUPLICATE_BARCODE`, 없는 SKU는 404 `SKU_NOT_FOUND`다.
- **비활성 SKU도 수정할 수 있다.**
- **단가 변경 이력**: 단가가 실제로 바뀐 수정마다 `sku_price_history`에 이전·이후 매입·공급 단가와 처리자(`changed_by`), 시각을 한 행으로 남긴다. 등록 시점의 초기 단가는 SKU 행이 곧 기록이라 남기지 않는다. 이번 범위에는 이력 조회 API가 없다.
- 이미 만든 창고 발주·지점 발주·출고 항목의 단가는 스냅샷이라 SKU 단가를 바꿔도 변하지 않는다(기존 규칙 그대로).
- 발주 등록의 `PURCHASE_PRICE_MISSING`·`SUPPLY_PRICE_MISSING` 검사와 출고 확정의 SKU 단가 대체 경로는 지우지 않고 방어용으로 남긴다. SKU 단가가 NOT NULL이 되어 정상 경로에서는 도달하지 않는다.

## 근거
- 단가가 비어 있으면 발주 단계에서야 막히고 복구 수단도 없었다. 등록 시점에 막고, 이미 빈 데이터는 수정 API로 채울 수 있게 해서 막다른 길을 없앤다.
- 수정 API를 단가 전용으로 쪼개지 않고 일반 SKU 수정으로 합쳐 보류 목록의 "SKU 수정"을 함께 해소한다. 상품 수정(`PATCH /products/{productId}`)과 같은 부분 수정 규칙을 쓴다.
- 단가는 원가·공급 금액의 기준이라 누가 언제 어떻게 바꿨는지 추적이 필요하다. 상태 전이용 `StatusHistory`와 성격이 달라 별도 테이블로 둔다.
- `unit`은 재고·발주 수량의 의미를 바꾸므로 수정 대상에서 뺐다. 필요해지면 별도로 결정한다.

## 검토했던 대안
- 수정 API를 단가 전용(`PATCH .../prices`)으로 분리 — 기각. 보류 목록의 SKU 수정이 따로 남는다.
- DB는 NULL 허용을 유지하고 API에서만 필수로 — 기각. 기존 NULL 행이 남아 같은 문제가 재발한다. 대신 기존 NULL은 0으로 채운다.
- 기존 NULL을 채우지 않고 마이그레이션을 실패시켜 정리를 강제 — 기각. 배포가 막히고, 정리할 실제 값이 없어 어차피 임의 값을 넣게 된다.
- 단가 변경 이력을 남기지 않음 — 기각. 원가 기준 변경을 추적할 수 없다.
- 단가 0을 막기 — 기각(결정). 현재 0 이상 규칙을 유지한다.

## 영향
- **0원 단가 주의**: 기존에 단가가 비어 있던 SKU는 마이그레이션 뒤 0이 되고, 0은 발주 단가로도 통과한다. 적용 후 수정 API로 실제 단가를 넣어야 한다. 적용 전 확인 쿼리는 V16 파일 상단에 있다.
- 클라이언트의 SKU 등록 화면은 두 단가를 필수 입력으로 바꾸고, SKU 수정 화면(단가 포함)이 필요하다.
- `ProductSkuUseCase.updateSku(command, actor)`, `SkuPriceHistoryRepository` 포트, `V16` 마이그레이션이 추가된다.
- 단가 이력을 읽는 API와 단가 변경 사유 입력은 이번 범위에 없다.
- 문서 갱신: `api/product.md`, `domain/pricing.md`.
