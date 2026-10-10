# 인가 규칙과 역할 매트릭스

엔드포인트별로 어떤 역할이 호출할 수 있고, 어떤 소속·작성자 검사가 붙는지 한 곳에 모은 표다. 각 도메인 API 명세의 권한 항목과 [ADR-003](../adr/003-master-data-owned-by-hq.md)(마스터 데이터는 본사만)을 정리한 것이며, 값이 다르면 도메인 명세가 기준이다. 검사를 어디서 하는지는 [ADR-012](../adr/012-authorization-check-placement.md)를 따른다. #170에서 모든 도메인에 적용했다.

## 검사 위치

| 검사 | 위치 | 실패 응답 |
|---|---|---|
| 인증 여부 (토큰 없음·만료·위조) | `SecurityConfig` | 401 `UNAUTHORIZED` |
| 역할 (이 API를 부를 수 있는 역할인가) | `SecurityConfig`의 경로·메서드 규칙 | 403 `FORBIDDEN` |
| 소속 범위 (담당 창고·지점의 리소스인가) | 서비스, 대상 리소스를 읽은 뒤 `AuthenticatedUser.requireWarehouseAccess/requireStoreAccess` | 403 `FORBIDDEN` |
| 작성자·상태 조건 (작성자만 취소 등) | 서비스 | 403 `FORBIDDEN` |

## 검사 순서와 403/404 규칙
- 상태를 바꾸는 요청의 검사 순서는 **대상 조회(없으면 404) → 소속 범위(403) → 상태(409) → 작성자·상태별 세부 권한(403) → 입력 검증(400) → 연관 조건(409)** 이다. 소속을 상태보다 먼저 확인해, 담당이 아닌 사용자에게 다른 창고·지점 리소스의 상태가 응답으로 드러나지 않게 한다.
- ID 자체가 소속을 나타내는 조회는 대상 조회 없이 **먼저 403**이다. 담당이 아닌 사용자에게는 존재하지 않는 ID도 403이다: `GET /warehouses/{id}`, `GET /warehouses/{id}/sections`, `GET /stores/{id}`, 재고·로트별 재고·이력·안전재고 목록의 `warehouseId` 필터, 입고·발주·출고·할당 목록의 `warehouseId` 필터, `GET /orders/my`의 `storeId`·`warehouseId` 필터.
- 그 밖의 단건 조회·변경(구역, 재고, 발주, 입고, 출고, 할당, 지점 발주 등)과 재고 목록의 `sectionId` 필터(구역을 읽어 그 창고를 확인)는 리소스를 읽은 뒤 소속을 검사하므로 **없는 ID는 404, 있지만 담당이 아니면 403**이다. 이 두 방식이 섞여 있는 것은 알려진 불일치이며, 순차 ID로 존재 여부를 추측할 수 있다는 점을 감수한다. 한쪽으로 통일하려면 별도 결정이 필요하다.

## 토큰 없이 호출할 수 있는 요청
- `POST /auth/signup`, `POST /auth/login`
- API 문서(`/v3/api-docs`, `/v3/api-docs/**`, `/v3/api-docs.yaml`, `/swagger-ui/**`, `/swagger-ui.html`)와 헬스 체크(`/actuator/health`, `/actuator/health/**`)
- 위 밖의 모든 요청은 로그인이 필요하다(토큰 없음·만료·위조는 401). 이 표에 없는 경로도 마찬가지다.

## 기호
- **전체**: 인증된 모든 역할(HQ_ADMIN, WAREHOUSE_MANAGER, STORE_OWNER). 소속 범위 없음.
- **(담당)**: 해당 역할은 본인에게 배정된 창고(`warehouseIds`) 또는 지점(`storeIds`)의 리소스만. 범위 밖 403. HQ_ADMIN은 항상 전체.

## 인증 (auth)
| 엔드포인트 | 역할 |
|---|---|
| POST /auth/signup, POST /auth/login | 인증 없음 |
| GET /auth/me, PATCH /auth/me/password | 전체 (본인 고정) |
| GET /users, PATCH /users/{userId} | HQ_ADMIN |

## 상품 (product)
| 엔드포인트 | 역할 |
|---|---|
| GET 전부 (상품·브랜드·카테고리·옵션 그룹·SKU) | 전체. 점주(STORE_OWNER)는 활성 상품·SKU만 보며(목록은 활성으로 고정, 비활성 단건은 404), SKU 응답에서 매입가·안전재고가 빠진다 |
| POST/PATCH 전부 | HQ_ADMIN |

## 창고 (warehouse)
| 엔드포인트 | 역할 |
|---|---|
| GET /warehouses, /warehouses/sections, /warehouses/managers | HQ_ADMIN |
| GET /warehouses/{id}, /warehouses/sections/{id}, /warehouses/{id}/sections | HQ_ADMIN, WAREHOUSE_MANAGER(담당) |
| GET /warehouses/my | WAREHOUSE_MANAGER |
| GET /warehouses/management-types, /warehouses/section-types | 전체 |
| POST/PATCH/DELETE 나머지 (창고·구역·관리자 배정) | HQ_ADMIN |

## 지점 (store)
| 엔드포인트 | 역할 |
|---|---|
| GET /stores, /stores/managers | HQ_ADMIN |
| GET /stores/{id} | HQ_ADMIN, STORE_OWNER(담당) |
| GET /stores/my | STORE_OWNER |
| GET /stores/management-types | 전체 |
| POST/PATCH/DELETE 나머지 (지점·담당자 배정) | HQ_ADMIN |

## 재고 (inventory)
| 엔드포인트 | 역할 |
|---|---|
| GET /inventory, /inventory/by-lot, /inventory/low-stock, /inventory/transactions, /inventory/{id}, /inventory/{id}/transactions, /lots, /lots/{id} | HQ_ADMIN, WAREHOUSE_MANAGER(담당). 로트는 담당 창고에 재고가 있거나 입고 완료 이력이 있는 것만 보이며, 상세 응답의 재고·입고 이력도 담당 창고 항목만 남는다 |
| POST /inventory/adjustments | WAREHOUSE_MANAGER(담당). HQ_ADMIN 불가 |

## 입고 (inbound)
| 엔드포인트 | 역할 |
|---|---|
| POST/PATCH /suppliers 계열 | HQ_ADMIN |
| GET /suppliers, /suppliers/{id} | HQ_ADMIN, WAREHOUSE_MANAGER (소속 범위 없음. 창고 관리자에게는 활성 공급처만 보이며 비활성 공급처 단건은 404) |
| POST /purchase-orders | WAREHOUSE_MANAGER(담당) |
| GET /purchase-orders 계열 | HQ_ADMIN, WAREHOUSE_MANAGER(담당) |
| PATCH /purchase-orders/{id}/confirm | HQ_ADMIN |
| PATCH /purchase-orders/{id}/cancel | REQUESTED: 작성자 WAREHOUSE_MANAGER만 (본사 포함 타인 403) / CONFIRMED: HQ_ADMIN만 |
| POST /inbounds, PATCH /inbounds/{id}/inspect, /complete, /cancel | WAREHOUSE_MANAGER(담당). HQ_ADMIN 불가 |
| GET /inbounds 계열 | HQ_ADMIN, WAREHOUSE_MANAGER(담당) |

## 지점 발주 (storeorder)
| 엔드포인트 | 역할 |
|---|---|
| POST /orders | STORE_OWNER(담당 지점). 본사·창고 관리자 대리 등록 불가 |
| GET /orders | HQ_ADMIN |
| GET /orders/my | STORE_OWNER, WAREHOUSE_MANAGER (소속 범위로 좁힘) |
| GET /orders/{id}, /orders/{id}/details | HQ_ADMIN, STORE_OWNER(담당 지점), WAREHOUSE_MANAGER(담당 창고에 배정된 발주) |
| PATCH /orders/{id}/approve, /reject, POST /orders/assign | HQ_ADMIN |
| PATCH /orders/{id}/cancel | STORE_OWNER(작성자) / HQ_ADMIN |
| PATCH /orders/{id}/hold, /resume, /complete-partial | WAREHOUSE_MANAGER(담당 창고) |

## 출고·재고 할당 (outbound)
| 엔드포인트 | 역할 |
|---|---|
| POST /allocations, PATCH /allocations/{id}/release | WAREHOUSE_MANAGER(발주의 담당 창고) |
| GET /allocations 계열, GET /outbounds 계열 | HQ_ADMIN, WAREHOUSE_MANAGER(담당 창고). STORE_OWNER 불가 |
| POST /outbounds, PATCH /outbounds/{id}/picking/start, /picking/complete, /ship, /deliver, /cancel | WAREHOUSE_MANAGER(담당 창고). HQ_ADMIN 불가 |

## 메모
- 처리 사용자(`createdBy`, `changedBy`, `receivedBy` 등)는 요청 파라미터가 아니라 토큰의 `userId`로 채운다. `userId` 쿼리 파라미터는 받지 않는다(#170에서 제거).
- 역할·계정 상태·소속은 토큰이 아니라 요청마다 DB에서 읽는다. 소속·역할 변경과 계정 비활성화(ACTIVE가 아니면 401)가 다음 요청부터 반영된다([ADR-016](../adr/016-reload-user-per-request.md)).
