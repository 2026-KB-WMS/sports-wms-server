#!/usr/bin/env python3
"""WMS e2e 시드 + 시나리오 러너 (의존성 없음, Python 3.9+).

실행 중인 서버에 HTTP로만 접근한다. 매 실행마다 접미사(RUN)를 붙여 새 데이터를 만들므로
같은 DB에 여러 번 돌려도 코드 중복으로 깨지지 않는다.

    python scripts/e2e/e2e_scenarios.py --base-url http://localhost:8080 \
        --admin-id e2e_admin --admin-password 'E2eAdmin!234'

서버는 WMS_ADMIN_LOGIN_ID / WMS_ADMIN_PASSWORD 등을 채워 띄워야 관리자가 생긴다.
종료 코드: 0 = 전부 통과, 1 = 하나 이상 실패, 2 = 시드 단계에서 중단.
"""
import argparse
import json
import sys
import time
import urllib.error
import urllib.request

RUN = time.strftime("%m%d%H%M%S")
PASSWORD = "E2ePass!2345"

results = []  # (name, ok, detail)


class Abort(Exception):
    pass


class Api:
    def __init__(self, base):
        self.base = base.rstrip("/") + "/api/v1"

    def call(self, method, path, body=None, token=None):
        data = json.dumps(body).encode("utf-8") if body is not None else None
        req = urllib.request.Request(self.base + path, data=data, method=method)
        req.add_header("Content-Type", "application/json")
        if token:
            req.add_header("Authorization", "Bearer " + token)
        try:
            with urllib.request.urlopen(req, timeout=30) as resp:
                status, raw = resp.status, resp.read()
        except urllib.error.HTTPError as e:
            status, raw = e.code, e.read()
        try:
            payload = json.loads(raw.decode("utf-8")) if raw else {}
        except ValueError:
            payload = {"raw": raw.decode("utf-8", "replace")}
        return status, payload


def check(name, ok, detail=""):
    results.append((name, bool(ok), detail))
    print(("  PASS  " if ok else "  FAIL  ") + name + ("" if ok else "   <- " + str(detail)))
    return ok


def expect(name, resp, status, error_code=None):
    code, body = resp
    ok = code == status and (error_code is None or body.get("errorCode") == error_code)
    check(name, ok, "HTTP %s %s" % (code, body.get("errorCode") or body.get("message") or ""))
    return body.get("data") if ok else None


def must(name, resp, status=200):
    """시드용: 기대 상태가 아니면 즉시 중단."""
    code, body = resp
    if code != status:
        raise Abort("%s: HTTP %s %s" % (name, code, json.dumps(body, ensure_ascii=False)[:300]))
    print("  seed  " + name)
    return body.get("data")


def section(title):
    print("\n== " + title)


def run(api, admin_id, admin_pw):
    # ---------------------------------------------------------------- 로그인·사용자
    section("1. 시드: 사용자·소속·승인")
    code, body = api.call("POST", "/auth/login", {"loginId": admin_id, "password": admin_pw})
    if code != 200:
        raise Abort("관리자 로그인 실패 HTTP %s %s (WMS_ADMIN_* 환경변수로 서버를 띄웠는지 확인)" % (code, body.get("message")))
    admin = body["data"]["accessToken"]

    def signup(prefix, role):
        d = must("signup " + prefix, api.call("POST", "/auth/signup", {
            "loginId": "%s_%s" % (prefix, RUN), "password": PASSWORD, "name": prefix + " " + RUN,
            "email": "%s_%s@e2e.test" % (prefix, RUN), "phone": "010-0000-0000", "role": role}), 201)
        return d["userId"], "%s_%s" % (prefix, RUN)

    wm1_id, wm1_login = signup("wm1", "WAREHOUSE_MANAGER")
    wm2_id, wm2_login = signup("wm2", "WAREHOUSE_MANAGER")
    own1_id, own1_login = signup("own1", "STORE_OWNER")
    own2_id, own2_login = signup("own2", "STORE_OWNER")

    # ---------------------------------------------------------------- 창고·구역
    w1 = must("창고 W1", api.call("POST", "/warehouses", {
        "warehouseCode": "W1-" + RUN, "warehouseName": "E2E 창고1", "address": "서울", "totalCapacity": 10000}, token=admin), 201)["warehouseId"]
    w2 = must("창고 W2", api.call("POST", "/warehouses", {
        "warehouseCode": "W2-" + RUN, "warehouseName": "E2E 창고2", "address": "부산", "totalCapacity": 1000}, token=admin), 201)["warehouseId"]

    def new_section(wid, code, name, typ, cap):
        return must("구역 " + code, api.call("POST", "/warehouses/sections", {
            "warehouseId": wid, "sectionCode": code, "sectionName": name, "sectionType": typ, "capacity": cap}, token=admin), 201)["sectionId"]

    sec_a = new_section(w1, "A-" + RUN, "일반A", "ZONE", 3000)
    sec_b = new_section(w1, "B-" + RUN, "일반B", "ZONE", 3000)
    sec_d = new_section(w1, "D-" + RUN, "불량", "DEFECT", 500)
    new_section(w2, "A2-" + RUN, "일반A2", "ZONE", 500)

    # ---------------------------------------------------------------- 지점
    s1 = must("지점 S1", api.call("POST", "/stores", {
        "storeCode": "S1-" + RUN, "storeName": "E2E 지점1", "address": "서울", "contactNumber": "02-000-0000"}, token=admin), 201)["storeId"]
    s2 = must("지점 S2", api.call("POST", "/stores", {
        "storeCode": "S2-" + RUN, "storeName": "E2E 지점2", "address": "대구", "contactNumber": "053-000-0000"}, token=admin), 201)["storeId"]

    # 소속 배정 후 승인 (승인 전에 소속이 있어야 ACTIVE 가능)
    must("W1 ← wm1", api.call("POST", "/warehouses/managers", {"warehouseId": w1, "userId": wm1_id, "memberRole": "MANAGER"}, token=admin), 201)
    must("W2 ← wm2", api.call("POST", "/warehouses/managers", {"warehouseId": w2, "userId": wm2_id, "memberRole": "MANAGER"}, token=admin), 201)
    must("S1 ← own1", api.call("POST", "/stores/assign", {"storeId": s1, "userId": own1_id, "memberRole": "OWNER"}, token=admin), 201)
    must("S2 ← own2", api.call("POST", "/stores/assign", {"storeId": s2, "userId": own2_id, "memberRole": "OWNER"}, token=admin), 201)
    for uid in (wm1_id, wm2_id, own1_id, own2_id):
        must("승인 user %s" % uid, api.call("PATCH", "/users/%s" % uid, {"status": "ACTIVE"}, token=admin))

    def login(login_id):
        return must("login " + login_id, api.call("POST", "/auth/login", {"loginId": login_id, "password": PASSWORD}))["accessToken"]

    wm1, wm2, own1, own2 = login(wm1_login), login(wm2_login), login(own1_login), login(own2_login)

    # ---------------------------------------------------------------- 상품·공급처
    section("2. 시드: 상품·공급처")
    brand = must("브랜드", api.call("POST", "/products/brands", {"brandName": "E2E브랜드" + RUN, "description": "e2e"}, token=admin), 201)["brandId"]
    cat = must("카테고리", api.call("POST", "/products/categories", {
        "categoryCode": "CAT-" + RUN, "categoryName": "E2E카테고리", "sortOrder": 1}, token=admin), 201)["categoryId"]
    prod = must("상품", api.call("POST", "/products", {
        "brandId": brand, "categoryId": cat, "productCode": "P-" + RUN, "productName": "E2E 상품"}, token=admin), 201)["productId"]
    grp = must("옵션 그룹", api.call("POST", "/products/option-groups", {"name": "사이즈" + RUN}, token=admin), 201)["optionGroupId"]
    val = must("옵션 값", api.call("POST", "/products/option-groups/%s/values" % grp, {"value": "M", "sortOrder": 1}, token=admin), 201)["optionValueId"]

    def new_sku(code, name, buy, sell):
        return must("SKU " + code, api.call("POST", "/products/skus", {
            "productId": prod, "skuCode": code + "-" + RUN, "skuName": name,
            "currentPurchasePrice": buy, "currentSupplyPrice": sell, "safetyStockQuantity": 5}, token=admin), 201)["skuId"]

    sku1 = new_sku("SKU1", "E2E SKU1", 1000, 1500)
    sku2 = new_sku("SKU2", "E2E SKU2", 2000, 3000)
    must("SKU1 옵션 연결", api.call("POST", "/products/skus/%s/options" % sku1, {"optionValueIds": [val]}, token=admin), 201)
    supplier = must("공급처", api.call("POST", "/suppliers", {
        "supplierCode": "SUP-" + RUN, "supplierName": "E2E공급처", "managerName": "담당", "contactNumber": "02-111-1111"}, token=admin), 201)["supplierId"]

    # ---------------------------------------------------------------- 시나리오 A: 입고
    section("3. 시나리오 A: 창고 발주 → 입고 → 재고 반영")
    po = must("발주 등록", api.call("POST", "/purchase-orders", {
        "warehouseId": w1, "supplierId": supplier,
        "lines": [{"skuId": sku1, "expectedQuantity": 100}, {"skuId": sku2, "expectedQuantity": 20}]}, token=wm1), 201)
    po_id = po["purchaseOrderId"]
    lines = {l["skuId"]: l for l in po["lines"]}
    check("발주 등록 상태 REQUESTED", po["status"] == "REQUESTED", po["status"])
    expect("창고 관리자는 발주 확정 불가(403)", api.call("PATCH", "/purchase-orders/%s/confirm" % po_id, token=wm1), 403)
    expect("본사 발주 확정", api.call("PATCH", "/purchase-orders/%s/confirm" % po_id, token=admin), 200)

    ib = must("입고 등록", api.call("POST", "/inbounds", {"purchaseOrderId": po_id}, token=wm1), 201)["inboundId"]
    expect("같은 발주에 진행 중 입고 중복 등록(409)", api.call("POST", "/inbounds", {"purchaseOrderId": po_id}, token=wm1), 409, "INBOUND_IN_PROGRESS")
    expect("assignable-sections 조회", api.call("GET", "/inbounds/%s/assignable-sections" % ib, token=wm1), 200)
    expect("검수 전 완료 불가(409)", api.call("PATCH", "/inbounds/%s/complete" % ib, token=wm1), 409)
    insp = {"lines": [
        {"purchaseOrderLineId": lines[sku1]["purchaseOrderLineId"], "lotNumber": "LOT-A-" + RUN,
         "manufacturedDate": "2026-01-01", "expiryDate": "2027-12-31", "receivedQuantity": 100,
         "acceptedQuantity": 98, "defectiveQuantity": 2, "receivedUnitPrice": 1000,
         "acceptedSectionId": sec_a, "defectSectionId": sec_d},
        {"purchaseOrderLineId": lines[sku2]["purchaseOrderLineId"], "lotNumber": "LOT-B-" + RUN,
         "expiryDate": "2028-06-30", "receivedQuantity": 20, "acceptedQuantity": 20,
         "defectiveQuantity": 0, "receivedUnitPrice": 2000, "acceptedSectionId": sec_b}]}
    expect("검수(inspect)", api.call("PATCH", "/inbounds/%s/inspect" % ib, insp, token=wm1), 200)
    bad = json.loads(json.dumps(insp))
    bad["lines"][0]["acceptedQuantity"] = 90  # 합 불일치
    expect("합격+불량≠입고 수량(400)", api.call("PATCH", "/inbounds/%s/inspect" % ib, bad, token=wm1), 400)
    done = expect("입고 완료", api.call("PATCH", "/inbounds/%s/complete" % ib, token=wm1), 200)
    if done:
        check("발주 COMPLETED", done["purchaseOrder"]["status"] == "COMPLETED", done["purchaseOrder"])
    expect("완료 재호출(409)", api.call("PATCH", "/inbounds/%s/complete" % ib, token=wm1), 409)

    def by_lot(sku):
        c, b = api.call("GET", "/inventory/by-lot?warehouseId=%s&skuId=%s" % (w1, sku), token=wm1)
        return b.get("data", {}).get("items", []) if c == 200 else []

    def row(sku, quality="AVAILABLE"):
        return next((r for r in by_lot(sku) if r["qualityStatus"] == quality), None)

    r1, r1d, r2 = row(sku1), row(sku1, "DEFECTIVE"), row(sku2)
    check("SKU1 합격 98 적치", r1 and r1["onHandQuantity"] == 98, r1)
    check("SKU1 불량 2 적치", r1d and r1d["onHandQuantity"] == 2, r1d)
    check("SKU2 합격 20 적치", r2 and r2["onHandQuantity"] == 20, r2)
    expect("GET /inventory 집계", api.call("GET", "/inventory?warehouseId=%s" % w1, token=admin), 200)

    # ---------------------------------------------------------------- 시나리오 B: 정상 발주→출고
    section("4. 시나리오 B: 지점 발주 → 승인 → 배정 → 할당 → 피킹 → 배송 → 완료")

    def new_order(token, store, items):
        return must("지점 발주", api.call("POST", "/orders", {
            "storeId": store, "lines": [{"skuId": s, "requestedQuantity": q} for s, q in items]}, token=token), 201)["storeOrderId"]

    def approve_assign(oid):
        must("승인 %s" % oid, api.call("PATCH", "/orders/%s/approve" % oid, token=admin))
        must("배정 %s" % oid, api.call("POST", "/orders/assign", {"storeOrderId": oid, "warehouseId": w1}, token=admin))

    o1 = new_order(own1, s1, [(sku1, 30), (sku2, 5)])
    approve_assign(o1)
    expect("ASSIGNED 이전 발주를 점주가 취소하지 못함(403)", api.call("PATCH", "/orders/%s/cancel" % o1, {"reason": "x"}, token=own1), 403)
    al = expect("재고 할당", api.call("POST", "/allocations", {"storeOrderId": o1}, token=wm1), 201)
    if al:
        check("할당 2건", len(al["items"]) == 2, len(al["items"]))
    expect("같은 요청 재할당(409 ALREADY_ALLOCATED)", api.call("POST", "/allocations", {"storeOrderId": o1}, token=wm1), 409, "ALREADY_ALLOCATED")
    ob = must("출고 생성", api.call("POST", "/outbounds", {"storeOrderId": o1}, token=wm1), 201)
    ob_id = ob["outboundId"]
    must("피킹 시작", api.call("PATCH", "/outbounds/%s/picking/start" % ob_id, token=wm1))
    expect("피킹 중 발주 취소 불가(409 ORDER_IN_PICKING)", api.call("PATCH", "/orders/%s/cancel" % o1, {"reason": "x"}, token=admin), 409, "ORDER_IN_PICKING")
    pk = {"lines": [{"outboundLineId": i["outboundLineId"], "pickedQuantity": i["allocatedQuantity"]} for i in ob["items"]]}
    expect("피킹 완료", api.call("PATCH", "/outbounds/%s/picking/complete" % ob_id, pk, token=wm1), 200)
    expect("배송 시작", api.call("PATCH", "/outbounds/%s/ship" % ob_id, token=wm1), 200)
    dv = expect("배송 완료", api.call("PATCH", "/outbounds/%s/deliver" % ob_id, token=wm1), 200)
    if dv:
        check("발주 COMPLETED(전량 출고)", dv["storeOrder"]["status"] == "COMPLETED", dv["storeOrder"])
    r1, r2 = row(sku1), row(sku2)
    check("SKU1 보유 68", r1 and r1["onHandQuantity"] == 68 and r1["allocatedQuantity"] == 0, r1)
    check("SKU2 보유 15", r2 and r2["onHandQuantity"] == 15 and r2["allocatedQuantity"] == 0, r2)
    det = expect("발주 상세 조회(점주)", api.call("GET", "/orders/%s/details" % o1, token=own1), 200)
    if det:
        hist = det["statusHistory"]
        check("상태 이력에 처리자 이름", hist and all(h.get("changedByName") for h in hist), hist)
    expect("내 발주 목록(점주)", api.call("GET", "/orders/my", token=own1), 200)

    # ---------------------------------------------------------------- 시나리오 C: 재고 부족·보류·취소
    section("5. 시나리오 C: 재고 부족 / 보류·재개 / 본사 취소")
    o2 = new_order(own1, s1, [(sku1, 100)])
    approve_assign(o2)
    al2 = expect("재고 부족 부분 할당(201)", api.call("POST", "/allocations", {"storeOrderId": o2}, token=wm1), 201)
    if al2:
        sh = al2.get("shortages") or []
        check("SKU1 요청 100 / 할당 68 / 부족 32 보고",
              len(sh) == 1 and sh[0]["skuId"] == sku1 and sh[0]["requestedQuantity"] == 100
              and sh[0]["allocatedQuantity"] == 68 and sh[0]["shortageQuantity"] == 32, sh)
    expect("보류", api.call("PATCH", "/orders/%s/hold" % o2, {"reason": "재고 확보 중"}, token=wm1), 200)
    expect("보류 중 할당 불가(409)", api.call("POST", "/allocations", {"storeOrderId": o2}, token=wm1), 409)
    expect("재개", api.call("PATCH", "/orders/%s/resume" % o2, {"reason": "재개"}, token=wm1), 200)
    # SKU1 가용이 0인 상태: 전량 부족이면 아무것도 할당하지 않고 409
    o5 = new_order(own1, s1, [(sku1, 1)])
    approve_assign(o5)
    expect("가용 0이면 409 INSUFFICIENT_STOCK", api.call("POST", "/allocations", {"storeOrderId": o5}, token=wm1), 409, "INSUFFICIENT_STOCK")
    expect("전량 부족 발주 취소", api.call("PATCH", "/orders/%s/cancel" % o5, {"reason": "재고 없음"}, token=admin), 200)
    c = expect("본사 취소(부분 할당분 해제)", api.call("PATCH", "/orders/%s/cancel" % o2, {"reason": "테스트 취소"}, token=admin), 200)
    if c:
        check("취소 상태 CANCELED", c["status"] == "CANCELED", c["status"])

    o4 = new_order(own1, s1, [(sku2, 2)])
    approve_assign(o4)
    expect("할당(취소 대상)", api.call("POST", "/allocations", {"storeOrderId": o4}, token=wm1), 201)
    c = expect("할당 후 본사 취소", api.call("PATCH", "/orders/%s/cancel" % o4, {"reason": "취소"}, token=admin), 200)
    if c:
        check("할당 1건 해제", c.get("releasedAllocationCount") == 1, c)
    r2 = row(sku2)
    check("취소 후 SKU2 할당 0", r2 and r2["allocatedQuantity"] == 0, r2)

    # SKU를 골라 할당: 선택하지 않은 SKU1(가용 68이지만 이 호출 대상 아님)은 건드리지 않는다
    o6 = new_order(own1, s1, [(sku2, 1), (sku1, 1)])
    approve_assign(o6)
    expect("발주에 없는 SKU 지정(400)", api.call("POST", "/allocations", {"storeOrderId": o6, "skuIds": [999999999]}, token=wm1), 400)
    expect("빈 skuIds(400)", api.call("POST", "/allocations", {"storeOrderId": o6, "skuIds": []}, token=wm1), 400)
    al6 = expect("SKU2만 선택 할당(201)", api.call("POST", "/allocations", {"storeOrderId": o6, "skuIds": [sku2]}, token=wm1), 201)
    if al6:
        check("선택한 SKU2 할당 1건만", len(al6["items"]) == 1 and al6["items"][0]["skuId"] == sku2, al6["items"])
    expect("선택한 SKU 재할당(409 ALREADY_ALLOCATED)", api.call("POST", "/allocations", {"storeOrderId": o6, "skuIds": [sku2]}, token=wm1), 409, "ALREADY_ALLOCATED")
    expect("선택 할당 발주 취소", api.call("PATCH", "/orders/%s/cancel" % o6, {"reason": "취소"}, token=admin), 200)

    # ---------------------------------------------------------------- 시나리오 D: 부분 출고·부분 종결
    section("6. 시나리오 D: 부분 피킹 → 후속 처리 → 부분 종결")
    o3 = new_order(own1, s1, [(sku1, 60)])
    approve_assign(o3)
    expect("할당", api.call("POST", "/allocations", {"storeOrderId": o3}, token=wm1), 201)
    ob3 = must("출고 생성", api.call("POST", "/outbounds", {"storeOrderId": o3}, token=wm1), 201)
    must("피킹 시작", api.call("PATCH", "/outbounds/%s/picking/start" % ob3["outboundId"], token=wm1))
    ln = ob3["items"][0]
    expect("피킹 수량 초과(400)", api.call("PATCH", "/outbounds/%s/picking/complete" % ob3["outboundId"], {
        "lines": [{"outboundLineId": ln["outboundLineId"], "pickedQuantity": ln["allocatedQuantity"] + 1}]}, token=wm1), 400)
    expect("부분 피킹 50/60", api.call("PATCH", "/outbounds/%s/picking/complete" % ob3["outboundId"], {
        "lines": [{"outboundLineId": ln["outboundLineId"], "pickedQuantity": 50}]}, token=wm1), 200)
    must("배송 시작", api.call("PATCH", "/outbounds/%s/ship" % ob3["outboundId"], token=wm1))
    dv = must("배송 완료", api.call("PATCH", "/outbounds/%s/deliver" % ob3["outboundId"], token=wm1))
    check("부분 출고라 발주는 ASSIGNED 유지", dv["storeOrder"]["status"] == "ASSIGNED", dv["storeOrder"])
    cp = expect("부분 종결", api.call("PATCH", "/orders/%s/complete-partial" % o3, {"reason": "재고 부족"}, token=wm1), 200)
    if cp:
        check("부족 수량 10", cp["items"][0]["shortageQuantity"] == 10, cp["items"])
    check("SKU1 보유 18", (row(sku1) or {}).get("onHandQuantity") == 18, row(sku1))

    o5 = new_order(own1, s1, [(sku2, 1)])
    expect("반려", api.call("PATCH", "/orders/%s/reject" % o5, {"reason": "테스트 반려"}, token=admin), 200)
    o6 = new_order(own1, s1, [(sku2, 1)])
    expect("점주 승인 전 취소", api.call("PATCH", "/orders/%s/cancel" % o6, token=own1), 200)

    # ---------------------------------------------------------------- 시나리오 E: 로트·재고 조정
    section("7. 시나리오 E: 로트 상태 변경 / 재고 조정")
    lot_id = r1["lotId"]
    expect("창고 관리자 로트 상태 변경 불가(403)", api.call("PATCH", "/lots/%s/status" % lot_id, {"status": "QUARANTINED", "reason": "x"}, token=wm1), 403)
    expect("EXPIRED로 변경 불가(400)", api.call("PATCH", "/lots/%s/status" % lot_id, {"status": "EXPIRED", "reason": "x"}, token=admin), 400)
    expect("격리", api.call("PATCH", "/lots/%s/status" % lot_id, {"status": "QUARANTINED", "reason": "품질 확인"}, token=admin), 200)
    o7 = new_order(own1, s1, [(sku1, 1)])
    approve_assign(o7)
    expect("격리 로트는 할당 불가(409)", api.call("POST", "/allocations", {"storeOrderId": o7}, token=wm1), 409)
    expect("발주 취소 정리", api.call("PATCH", "/orders/%s/cancel" % o7, {"reason": "정리"}, token=admin), 200)
    expect("재고 있는 로트 폐기 불가(409 LOT_HAS_STOCK)", api.call("PATCH", "/lots/%s/status" % lot_id, {"status": "DISPOSED", "reason": "x"}, token=admin), 409, "LOT_HAS_STOCK")
    expect("격리 해제", api.call("PATCH", "/lots/%s/status" % lot_id, {"status": "AVAILABLE", "reason": "이상 없음"}, token=admin), 200)

    r2 = row(sku2)
    adj = {"inventoryLotId": r2["inventoryLotId"], "beforeQuantity": r2["onHandQuantity"],
           "afterQuantity": r2["onHandQuantity"] - 1, "reason": "실사 차이"}
    expect("재고 조정", api.call("POST", "/inventory/adjustments", adj, token=wm1), 201)
    expect("오래된 beforeQuantity(409 STALE_QUANTITY)", api.call("POST", "/inventory/adjustments", adj, token=wm1), 409, "STALE_QUANTITY")
    expect("재고 이력 조회", api.call("GET", "/inventory/%s/transactions" % r2["inventoryLotId"], token=wm1), 200)

    # ---------------------------------------------------------------- 시나리오 F: 권한·범위
    section("8. 시나리오 F: 권한·소속 범위")
    expect("토큰 없음(401)", api.call("GET", "/warehouses"), 401)
    expect("점주는 창고 목록 불가(403)", api.call("GET", "/warehouses", token=own1), 403)
    expect("점주는 재고 조회 불가(403)", api.call("GET", "/inventory", token=own1), 403)
    expect("창고 관리자는 발주 등록 불가(403)", api.call("POST", "/orders", {"storeId": s1, "lines": [{"skuId": sku1, "requestedQuantity": 1}]}, token=wm1), 403)
    expect("점주가 타 지점 발주 등록(403)", api.call("POST", "/orders", {"storeId": s1, "lines": [{"skuId": sku1, "requestedQuantity": 1}]}, token=own2), 403)
    expect("타 창고 관리자는 출고 상세 불가(403)", api.call("GET", "/outbounds/%s/details" % ob_id, token=wm2), 403)
    expect("타 창고 관리자는 입고 조회 불가(403)", api.call("GET", "/inbounds/%s" % ib, token=wm2), 403)
    expect("창고 관리자는 마스터 수정 불가(403)", api.call("PATCH", "/products/brands/%s" % brand, {"brandName": "x"}, token=wm1), 403)
    expect("점주의 SKU 목록에 매입가 없음", api.call("GET", "/products/skus?productId=%s" % prod, token=own1), 200)
    code, body = api.call("GET", "/products/skus?productId=%s" % prod, token=own1)
    if code == 200:
        check("점주 응답에 currentPurchasePrice 없음", all("currentPurchasePrice" not in i for i in body["data"]["items"]), body["data"]["items"][:1])
    expect("존재하지 않는 창고 필터(404)", api.call("GET", "/purchase-orders?warehouseId=99999999", token=admin), 404, "WAREHOUSE_NOT_FOUND")
    expect("내 소속 창고", api.call("GET", "/warehouses/my", token=wm1), 200)
    expect("내 소속 지점", api.call("GET", "/stores/my", token=own1), 200)

    # ---------------------------------------------------------------- 시나리오 G: 마스터 데이터 관리
    section("9. 시나리오 G: 마스터 데이터 수정·삭제·비활성화 가드")
    expect("브랜드 수정", api.call("PATCH", "/products/brands/%s" % brand, {"brandName": "E2E브랜드수정" + RUN}, token=admin), 200)
    expect("카테고리 수정", api.call("PATCH", "/products/categories/%s" % cat, {"categoryName": "수정됨", "sortOrder": 3}, token=admin), 200)
    expect("카테고리 코드 수정 불가(400)", api.call("PATCH", "/products/categories/%s" % cat, {"categoryCode": "X"}, token=admin), 400)
    expect("SKU 단가 수정", api.call("PATCH", "/products/skus/%s" % sku1, {"currentSupplyPrice": 1600}, token=admin), 200)
    tmp = new_section(w2, "TMP-" + RUN, "임시", "ZONE", 10)
    expect("빈 구역 삭제", api.call("DELETE", "/warehouses/sections/%s" % tmp, token=admin), 200)
    expect("재고 있는 구역 삭제 불가(409)", api.call("DELETE", "/warehouses/sections/%s" % sec_a, token=admin), 409, "SECTION_HAS_INVENTORY")
    expect("재고 있는 상품 비활성화 불가(409)", api.call("PATCH", "/products/%s" % prod, {"isActive": False}, token=admin), 409, "PRODUCT_IN_USE")
    expect("재고 있는 창고 비활성화 불가(409)", api.call("PATCH", "/warehouses/%s/deactivate" % w1, token=admin), 409, "WAREHOUSE_IN_USE")
    sup2 = must("공급처2", api.call("POST", "/suppliers", {
        "supplierCode": "SUP2-" + RUN, "supplierName": "E2E공급처2", "managerName": "담당", "contactNumber": "02-222-2222"}, token=admin), 201)["supplierId"]
    expect("공급처 비활성화", api.call("PATCH", "/suppliers/%s/deactivate" % sup2, token=admin), 200)
    expect("창고 관리자 비활성 공급처 조회 404", api.call("GET", "/suppliers/%s" % sup2, token=wm1), 404, "SUPPLIER_NOT_FOUND")
    expect("공급처 재활성화", api.call("PATCH", "/suppliers/%s/activate" % sup2, token=admin), 200)
    expect("가입 신청 중복 로그인 ID(409)", api.call("POST", "/auth/signup", {
        "loginId": wm1_login, "password": PASSWORD, "name": "dup", "email": "dup_%s@e2e.test" % RUN,
        "phone": "010", "role": "STORE_OWNER"}), 409, "DUPLICATE_LOGIN_ID")


def main():
    # Windows 콘솔(cp949)에서도 한글이 깨지지 않게 한다.
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    ap = argparse.ArgumentParser()
    ap.add_argument("--base-url", default="http://localhost:8080")
    ap.add_argument("--admin-id", default="e2e_admin")
    ap.add_argument("--admin-password", default="E2eAdmin!234")
    args = ap.parse_args()
    api = Api(args.base_url)
    try:
        run(api, args.admin_id, args.admin_password)
    except Abort as e:
        print("\n중단: " + str(e))
        sys.exit(2)
    failed = [r for r in results if not r[1]]
    print("\n== 결과: %d 통과 / %d 실패 (RUN=%s)" % (len(results) - len(failed), len(failed), RUN))
    for name, _, detail in failed:
        print("  FAIL  %s   <- %s" % (name, detail))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
