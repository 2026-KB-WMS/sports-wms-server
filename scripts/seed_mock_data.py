#!/usr/bin/env python3
"""로컬 개발용 목데이터 시드 스크립트.

서버 API를 실제 업무 흐름대로 호출해서(가입 승인 → 창고·지점·구역 → 상품·SKU → 공급처 매핑 →
발주 요청·확정·입고 → 지점 발주·출고) 도메인 규칙과 상태 이력이 맞는 데이터를 만든다.
DB에 직접 INSERT 하지 않는다.

사용법 (서버가 http://localhost:8080 에서 실행 중이고 본사 관리자가 있어야 한다)
    python scripts/seed_mock_data.py                 # 기본 시드: 마스터 + 대표 업무 흐름 몇 건
    python scripts/seed_mock_data.py --bulk          # 업무 확인용 대량 데이터 추가(상품 46개, 발주 40건 등)
    python scripts/seed_mock_data.py --bulk --purchase-orders 100 --store-orders 150
    python scripts/seed_mock_data.py --masters-only  # 마스터 데이터만

기본 관리자 계정은 .env의 WMS_ADMIN_LOGIN_ID / WMS_ADMIN_PASSWORD를 읽는다
(--admin-id, --admin-password로 바꿀 수 있다).

만들어지는 계정: 비밀번호는 아이디 뒤에 '!'를 붙인 값이다.
    창고 관리자  manager01(서울), manager02(부산)         예) manager01 / manager01!
    점주        owner01~owner04(기본 지점), owner05~owner08(--bulk로 추가되는 지점)

마스터 데이터(사용자·창고·구역·지점·공급처·상품·매핑)는 이미 있으면 건너뛰고,
업무 흐름 데이터(발주·입고·지점 발주·출고)는 발주가 하나도 없을 때만 만든다.
--bulk는 실행할 때마다 발주·지점 발주를 추가하므로 초기화 후 한 번만 실행하는 것을 권한다.
같은 --seed 값이면 같은 데이터가 만들어진다. 생성·입고 일시는 서버가 찍는 값이라 모두 실행 시각이다.
처음부터 다시 만들려면 DB를 초기화한다: docker-compose down -v && docker-compose up -d --build
"""
import argparse
import json
import os
import sys
import urllib.error
import urllib.request
from datetime import date, datetime, timedelta

def pw(login_id):
    """시드 계정의 비밀번호는 아이디 + '!' (예: owner01 / owner01!)."""
    return login_id + '!'



class Api:
    def __init__(self, base_url):
        self.base = base_url.rstrip('/') + '/api/v1'
        self.token = None

    def call(self, method, path, body=None, token=None, expect_ok=True):
        data = None
        headers = {'Content-Type': 'application/json; charset=utf-8'}
        use_token = token or self.token
        if use_token:
            headers['Authorization'] = 'Bearer ' + use_token
        if body is not None:
            data = json.dumps(body, ensure_ascii=False).encode('utf-8')
        req = urllib.request.Request(self.base + path, data=data, method=method, headers=headers)
        try:
            with urllib.request.urlopen(req) as resp:
                payload = json.loads(resp.read().decode('utf-8') or '{}')
        except urllib.error.HTTPError as e:
            payload = json.loads(e.read().decode('utf-8') or '{}')
            if expect_ok:
                raise RuntimeError('%s %s -> %s %s %s' % (
                    method, path, e.code, payload.get('errorCode'), payload.get('message')))
            return payload
        return payload.get('data')

    def get(self, path, token=None):
        return self.call('GET', path, token=token)

    def post(self, path, body=None, token=None):
        return self.call('POST', path, body, token=token)

    def patch(self, path, body=None, token=None):
        return self.call('PATCH', path, body, token=token)

    def put(self, path, body=None, token=None):
        return self.call('PUT', path, body, token=token)

    def login(self, login_id, password):
        data = self.call('POST', '/auth/login', {'loginId': login_id, 'password': password}, token='')
        return data['accessToken']


def log(msg):
    print(msg, flush=True)


def read_env(name):
    path = os.path.join(os.path.dirname(__file__), '..', '.env')
    if not os.path.exists(path):
        return None
    for line in open(path, encoding='utf-8'):
        if line.startswith(name + '='):
            return line.split('=', 1)[1].strip()
    return None


# --------------------------------------------------------------------------------------
# 마스터 데이터 정의
# --------------------------------------------------------------------------------------
WAREHOUSES = [
    {'warehouseName': '서울 물류센터', 'address': '서울특별시 금천구 가산디지털1로 100', 'contactNumber': '02-1000-1000',
     'totalCapacity': 50000, 'latitude': 37.4785, 'longitude': 126.8826, 'prefix': 'SEL'},
    {'warehouseName': '부산 물류센터', 'address': '부산광역시 강서구 녹산산단 26로 30', 'contactNumber': '051-2000-2000',
     'totalCapacity': 30000, 'latitude': 35.0988, 'longitude': 128.8620, 'prefix': 'BSN'},
]

STORES = [
    {'storeName': '강남점', 'address': '서울특별시 강남구 테헤란로 152', 'contactName': '강남점장', 'contactNumber': '02-3000-0001',
     'latitude': 37.5006, 'longitude': 127.0364},
    {'storeName': '홍대점', 'address': '서울특별시 마포구 양화로 160', 'contactName': '홍대점장', 'contactNumber': '02-3000-0002',
     'latitude': 37.5575, 'longitude': 126.9245},
    {'storeName': '해운대점', 'address': '부산광역시 해운대구 해운대해변로 264', 'contactName': '해운대점장', 'contactNumber': '051-3000-0003',
     'latitude': 35.1587, 'longitude': 129.1604},
    {'storeName': '서면점', 'address': '부산광역시 부산진구 중앙대로 702', 'contactName': '서면점장', 'contactNumber': '051-3000-0004',
     'latitude': 35.1577, 'longitude': 129.0594},
]

SUPPLIERS = [
    {'supplierCode': 'SUP-001', 'supplierName': '요넥스코리아', 'managerName': '김요넥', 'contactNumber': '02-4000-0001',
     'email': 'sales@yonex-korea.example.com', 'address': '서울특별시 강남구 논현로 400'},
    {'supplierCode': 'SUP-002', 'supplierName': '빅터코리아', 'managerName': '이빅터', 'contactNumber': '02-4000-0002',
     'email': 'sales@victor-korea.example.com', 'address': '서울특별시 송파구 올림픽로 300'},
    {'supplierCode': 'SUP-003', 'supplierName': '리닝스포츠', 'managerName': '박리닝', 'contactNumber': '031-4000-0003',
     'email': 'sales@lining-sports.example.com', 'address': '경기도 성남시 분당구 판교로 200'},
    {'supplierCode': 'SUP-004', 'supplierName': '한국스포츠유통', 'managerName': '최유통', 'contactNumber': '031-4000-0004',
     'email': 'sales@ksports.example.com', 'address': '경기도 용인시 기흥구 흥덕1로 13'},
    {'supplierCode': 'SUP-005', 'supplierName': '구 거래처(비활성)', 'managerName': '정종료', 'contactNumber': '02-4000-0005',
     'email': None, 'address': None, 'deactivate': True},
]

# 브랜드 코드 → 공급처 코드 (공통 유통사 SUP-004는 모든 SKU와 매핑)
BRAND_SUPPLIER = {'YNX': 'SUP-001', 'VCT': 'SUP-002', 'LNG': 'SUP-003', 'TRI': 'SUP-004', 'APX': 'SUP-004',
                  'TCH': 'SUP-004'}

# (브랜드 코드, 카테고리 코드, 상품명, 설명, 매입가, 공급가, SKU 개수)
PRODUCTS = [
    ('YNX', 'RKT', '아스트록스 88D', '공격형 배드민턴 라켓', 120000, 168000, 3),
    ('YNX', 'RKT', '나노플레어 700', '스피드형 배드민턴 라켓', 110000, 154000, 3),
    ('VCT', 'RKT', '브레이브소드 12', '올라운드 배드민턴 라켓', 90000, 126000, 2),
    ('LNG', 'RKT', '윈드스톰 72', '헤드 헤비 배드민턴 라켓', 85000, 119000, 2),
    ('YNX', 'SHC', '에어로클럽 2000', '연습용 셔틀콕 12개입', 18000, 25200, 2),
    ('VCT', 'SHC', '마스터 No.1', '대회용 셔틀콕 12개입', 32000, 44800, 2),
    ('YNX', 'SHO', '파워쿠션 65Z', '배드민턴 전용 신발', 95000, 133000, 3),
    ('LNG', 'SHO', '써밋 라이트', '경량 배드민턴 신발', 72000, 100800, 2),
    ('TRI', 'APR', '팀 경기용 져지', '경기용 반팔 상의', 28000, 39200, 3),
    ('APX', 'BAG', '투어 백팩 6R', '라켓 6자루 수납 가방', 54000, 75600, 2),
    ('YNX', 'STR', 'BG65 스트링', '내구성 배드민턴 스트링', 9000, 12600, 2),
    ('VCT', 'GRP', 'AC102 오버그립', '오버 그립 3개입', 4500, 6300, 2),
]

# 사용자: (loginId, 이름, 이메일, 전화, 역할, 소속 종류, 소속 이름)
USERS = [
    ('manager01', '김서울', 'wm.seoul@example.com', '010-1111-0001', 'WAREHOUSE_MANAGER', 'warehouse', '서울 물류센터'),
    ('manager02', '박부산', 'wm.busan@example.com', '010-1111-0002', 'WAREHOUSE_MANAGER', 'warehouse', '부산 물류센터'),
    ('owner01', '이강남', 'owner.gangnam@example.com', '010-2222-0001', 'STORE_OWNER', 'store', '강남점'),
    ('owner02', '최홍대', 'owner.hongdae@example.com', '010-2222-0002', 'STORE_OWNER', 'store', '홍대점'),
    ('owner03', '정해운', 'owner.haeundae@example.com', '010-2222-0003', 'STORE_OWNER', 'store', '해운대점'),
    ('owner04', '한서면', 'owner.seomyeon@example.com', '010-2222-0004', 'STORE_OWNER', 'store', '서면점'),
]


def items_of(data):
    if isinstance(data, dict) and 'items' in data:
        return data['items']
    return data


# --------------------------------------------------------------------------------------
# 마스터 데이터
# --------------------------------------------------------------------------------------
def seed_masters(api, ctx):
    hq = ctx['hq']

    # 창고
    existing = {w['warehouseName']: w for w in items_of(api.get('/warehouses', hq))}
    ctx['warehouse'] = {}
    for w in WAREHOUSES:
        if w['warehouseName'] in existing:
            data = existing[w['warehouseName']]
        else:
            body = {k: v for k, v in w.items() if k != 'prefix'}
            data = api.post('/warehouses', body, hq)
            log('  창고 등록: %s' % w['warehouseName'])
        ctx['warehouse'][w['warehouseName']] = data['warehouseId']

    # 구역: ZONE(상위) + RACK 3개 + DEFECT 1개
    ctx['sections'] = {}
    for w in WAREHOUSES:
        wid = ctx['warehouse'][w['warehouseName']]
        current = {s['sectionCode']: s for s in items_of(api.get('/warehouses/%d/sections' % wid, hq))}
        pre = w['prefix']

        def section(code, name, kind, capacity, parent=None):
            if code in current:
                return current[code]['sectionId']
            body = {'warehouseId': wid, 'sectionCode': code, 'sectionName': name, 'sectionType': kind,
                    'capacity': capacity}
            if parent:
                body['parentSectionId'] = parent
            return api.post('/warehouses/sections', body, hq)['sectionId']

        zone = section('%s-A' % pre, 'A동', 'ZONE', 20000)
        racks = [section('%s-A-%02d' % (pre, n), 'A동 %d번 랙' % n, 'RACK', 5000, zone) for n in (1, 2, 3)]
        defect = section('%s-D-01' % pre, '불량 보관 구역', 'DEFECT', 1000)
        ctx['sections'][wid] = {'racks': racks, 'defect': defect}
    log('  구역 준비 완료')

    # 지점
    existing = {s['storeName']: s for s in items_of(api.get('/stores', hq))}
    ctx['store'] = {}
    for s in STORES:
        if s['storeName'] in existing:
            data = existing[s['storeName']]
        else:
            data = api.post('/stores', s, hq)
            log('  지점 등록: %s' % s['storeName'])
        ctx['store'][s['storeName']] = data['storeId']

    # 사용자: 가입 → 소속을 지정하며 승인(ACTIVE)
    users = {u['loginId']: u for u in items_of(api.get('/users', hq))}
    ctx['user'] = {}
    for login_id, name, email, phone, role, kind, target in USERS:
        if login_id not in users:
            api.post('/auth/signup', {'loginId': login_id, 'password': pw(login_id), 'name': name, 'email': email,
                                      'phone': phone, 'role': role}, token='')
            users[login_id] = [u for u in items_of(api.get('/users', hq)) if u['loginId'] == login_id][0]
            log('  사용자 가입: %s (%s)' % (login_id, role))
        user = users[login_id]
        ctx['user'][login_id] = user['userId']
        if user.get('status') != 'ACTIVE':
            body = {'status': 'ACTIVE'}
            if kind == 'warehouse':
                body['warehouseId'] = ctx['warehouse'][target]
            else:
                body['storeId'] = ctx['store'][target]
            api.patch('/users/%d' % user['userId'], body, hq)
            log('  사용자 승인·배정: %s -> %s' % (login_id, target))

    # 공급처
    existing = {s['supplierCode']: s for s in items_of(api.get('/suppliers', hq))}
    ctx['supplier'] = {}
    for s in SUPPLIERS:
        if s['supplierCode'] in existing:
            ctx['supplier'][s['supplierCode']] = existing[s['supplierCode']]['supplierId']
            continue
        body = {k: v for k, v in s.items() if k != 'deactivate' and v is not None}
        data = api.post('/suppliers', body, hq)
        ctx['supplier'][s['supplierCode']] = data['supplierId']
        log('  공급처 등록: %s' % s['supplierName'])
    # 비활성 공급처는 매핑·발주가 끝난 뒤 마지막에 비활성화한다.

    # 상품·SKU
    brands = {b['brandCode']: b['brandId'] for b in items_of(api.get('/products/brands', hq)) if b.get('brandCode')}
    categories = {c['categoryCode']: c['categoryId'] for c in items_of(api.get('/products/categories', hq))}
    existing_products = {p['productName']: p for p in items_of(api.get('/products', hq))}
    ctx['skus'] = []  # {'skuId','brand','category','name','purchase','supply'}
    for brand, cat, name, desc, purchase, supply, sku_count in PRODUCTS:
        groups = items_of(api.get('/products/categories/%d/option-groups' % categories[cat], hq))
        spec_ids = [g['values'][0]['optionValueId'] for g in groups if g['optionType'] == 'SPEC' and g['values']]
        sku_groups = [g for g in groups if g['optionType'] == 'SKU' and g['values']]
        if name in existing_products:
            product_id = existing_products[name]['productId']
            sku_rows = [s for s in items_of(api.get('/products/skus?productId=%d' % product_id, hq))]
            for row in sku_rows:
                ctx['skus'].append({'skuId': row['skuId'], 'brand': brand, 'category': cat, 'name': row.get('skuName') or name,
                                    'purchase': purchase, 'supply': supply})
            continue
        product = api.post('/products', {'brandId': brands[brand], 'categoryId': categories[cat], 'productName': name,
                                         'description': desc, 'specOptionValueIds': spec_ids}, hq)
        product_id = product['productId']
        for i in range(sku_count):
            option_ids = [g['values'][(i + j) % len(g['values'])]['optionValueId'] for j, g in enumerate(sku_groups)]
            sku = api.post('/products/skus', {
                'productId': product_id, 'currentPurchasePrice': purchase, 'currentSupplyPrice': supply,
                'unit': 'EA', 'safetyStockQuantity': 20, 'optionValueIds': option_ids}, hq)
            ctx['skus'].append({'skuId': sku['skuId'], 'brand': brand, 'category': cat, 'name': name,
                                'purchase': purchase, 'supply': supply})
        log('  상품 등록: %s (SKU %d개)' % (name, sku_count))

    # SKU-공급처 매핑: 브랜드 공급처 + 공통 유통사
    for sku in ctx['skus']:
        mapped = {m['supplierId'] for m in items_of(api.get('/products/skus/%d/suppliers' % sku['skuId'], hq))}
        for code in {BRAND_SUPPLIER[sku['brand']], 'SUP-004'}:
            sid = ctx['supplier'][code]
            if sid not in mapped:
                api.post('/products/skus/%d/suppliers' % sku['skuId'], {'supplierId': sid}, hq)
    log('  SKU-공급처 매핑 완료 (SKU %d개)' % len(ctx['skus']))


# --------------------------------------------------------------------------------------
# 업무 흐름
# --------------------------------------------------------------------------------------
def lot_no(ctx):
    ctx['lot_seq'] += 1
    return 'LOT-%s-%03d' % (date.today().strftime('%y%m%d'), ctx['lot_seq'])


def request_order(api, ctx, manager, warehouse_name, sku_quantities, note):
    token = ctx['token'][manager]
    lines = [{'skuId': sku, 'expectedQuantity': qty} for sku, qty in sku_quantities]
    po = api.post('/purchase-orders', {'warehouseId': ctx['warehouse'][warehouse_name], 'note': note,
                                       'lines': lines}, token)
    return po['purchaseOrderId']


def confirm_order(api, ctx, po_id, prefer_brand_supplier=True):
    """본사가 라인별 공급처를 고른다. 브랜드 공급처가 있으면 그것을, 없으면 공통 유통사."""
    hq = ctx['hq']
    details = api.get('/purchase-orders/%d/details' % po_id, hq)
    sku_by_id = {s['skuId']: s for s in ctx['skus']}
    lines = []
    for item in details['items']:
        brand = sku_by_id[item['skuId']]['brand']
        code = BRAND_SUPPLIER[brand] if prefer_brand_supplier else 'SUP-004'
        lines.append({'purchaseOrderLineId': item['purchaseOrderLineId'], 'supplierId': ctx['supplier'][code]})
    result = api.patch('/purchase-orders/%d/confirm' % po_id, {'lines': lines}, hq)
    return result['supplierOrders']


def receive(api, ctx, manager, warehouse_name, supplier_order_id, ratio=1.0, complete=True, defect_every=7):
    """공급처 발주서에 입고를 등록하고 검수한다. ratio는 잔여 수량 중 받는 비율."""
    token = ctx['token'][manager]
    wid = ctx['warehouse'][warehouse_name]
    sections = ctx['sections'][wid]
    inbound = api.post('/inbounds', {'supplierOrderId': supplier_order_id,
                                     'arrivedAt': (datetime.now() - timedelta(hours=2)).strftime('%Y-%m-%dT%H:%M:%S'),
                                     'note': '정상 도착'}, token)
    inbound_id = inbound['inboundId']
    detail = api.get('/supplier-orders/%d' % supplier_order_id, token)
    lines = []
    for idx, line in enumerate(detail['lines']):
        remaining = line['remainingQuantity']
        if remaining <= 0:
            continue
        qty = max(1, int(remaining * ratio))
        defect = 1 if (qty >= 5 and (idx + 1) % defect_every == 0) else 0
        lines.append({
            'purchaseOrderLineId': line['purchaseOrderLineId'],
            'lotNumber': lot_no(ctx),
            'manufacturedDate': (date.today() - timedelta(days=30)).isoformat(),
            'expiryDate': (date.today() + timedelta(days=730)).isoformat(),
            'receivedQuantity': qty,
            'acceptedQuantity': qty - defect,
            'defectiveQuantity': defect,
            'receivedUnitPrice': line['orderedUnitPrice'],
            'inspectionNote': '불량 1개 발견' if defect else None,
            'acceptedSectionId': sections['racks'][idx % len(sections['racks'])],
            'defectSectionId': sections['defect'],
        })
    api.patch('/inbounds/%d/inspect' % inbound_id, {'lines': lines}, token)
    if complete:
        api.patch('/inbounds/%d/complete' % inbound_id, None, token)
    return inbound_id


def seed_purchase_flows(api, ctx):
    hq = ctx['hq']
    skus = ctx['skus']

    def pick(n, offset=0, qty=100):
        return [(skus[(offset + i) % len(skus)]['skuId'], qty + 10 * ((offset + i) % 5)) for i in range(n)]

    # 서울: 대량 입고(여러 공급처), 전량 입고 완료
    po = request_order(api, ctx, 'manager01', '서울 물류센터', pick(10, 0, 120), '서울 정기 보충 발주 (전량 입고 완료)')
    for so in confirm_order(api, ctx, po):
        receive(api, ctx, 'manager01', '서울 물류센터', so['supplierOrderId'])
    log('  발주1: 서울 전량 입고 완료')

    # 서울: 일부 입고 후 남음(PARTIALLY_RECEIVED), 다른 공급처 발주서는 입고 진행 중
    po = request_order(api, ctx, 'manager01', '서울 물류센터', pick(8, 10, 80), '서울 신학기 대비 발주 (부분 입고)')
    orders = confirm_order(api, ctx, po)
    for i, so in enumerate(orders):
        if i == 0:
            receive(api, ctx, 'manager01', '서울 물류센터', so['supplierOrderId'], ratio=0.5)
        elif i == 1:
            receive(api, ctx, 'manager01', '서울 물류센터', so['supplierOrderId'], ratio=1.0, complete=False)
    log('  발주2: 서울 부분 입고 + 검수 중 입고')

    # 서울: 요청됨(본사 확정 대기), 반려
    request_order(api, ctx, 'manager01', '서울 물류센터', pick(4, 20, 60), '서울 추가 발주 (확정 대기)')
    rejected = request_order(api, ctx, 'manager01', '서울 물류센터', pick(3, 30, 500), '과다 수량 요청')
    api.patch('/purchase-orders/%d/reject' % rejected, {'reason': '요청 수량이 안전 재고 대비 과다합니다.'}, hq)
    log('  발주3~4: 서울 확정 대기, 반려')

    # 부산: 전량 입고, 확정 후 미입고, 취소
    po = request_order(api, ctx, 'manager02', '부산 물류센터', pick(8, 3, 100), '부산 정기 보충 발주 (전량 입고 완료)')
    for so in confirm_order(api, ctx, po):
        receive(api, ctx, 'manager02', '부산 물류센터', so['supplierOrderId'])
    po = request_order(api, ctx, 'manager02', '부산 물류센터', pick(5, 13, 70), '부산 추가 발주 (발행됨)')
    confirm_order(api, ctx, po)
    po = request_order(api, ctx, 'manager02', '부산 물류센터', pick(3, 18, 50), '부산 발주 (취소)')
    confirm_order(api, ctx, po, prefer_brand_supplier=False)
    api.patch('/purchase-orders/%d/cancel' % po, {'reason': '공급처 사정으로 납품 불가'}, hq)
    log('  발주5~7: 부산 전량 입고, 발행됨, 취소')


def seed_store_orders(api, ctx):
    hq = ctx['hq']
    # 배정할 창고에 재고가 있는 SKU만 주문한다(창고별 가용 재고 기준)
    pools = {}
    for name, wid in ctx['warehouse'].items():
        rows = items_of(api.get('/inventory?warehouseId=%d' % wid, hq))
        pools[name] = [r['skuId'] for r in rows if r.get('availableQuantity', 0) >= 20]
        if not pools[name]:
            log('  %s에 재고가 없어 지점 발주를 만들지 않습니다.' % name)
            return

    def order(owner, store_name, warehouse_name, picks, note):
        token = ctx['token'][owner]
        pool = pools[warehouse_name]
        lines = [{'skuId': pool[i % len(pool)], 'requestedQuantity': qty} for i, qty in picks]
        return api.post('/orders', {'storeId': ctx['store'][store_name],
                                    'requestedDeliveryAt': (datetime.now() + timedelta(days=3)).strftime('%Y-%m-%dT%H:%M:%S'),
                                    'note': note, 'lines': lines}, token)['storeOrderId']

    def approve_assign(order_id, warehouse_name):
        api.patch('/orders/%d/approve' % order_id, None, hq)
        api.post('/orders/assign', {'storeOrderId': order_id, 'warehouseId': ctx['warehouse'][warehouse_name],
                                    'reason': '가까운 물류센터 배정'}, hq)

    def allocate_and_create(order_id, manager):
        token = ctx['token'][manager]
        api.post('/allocations', {'storeOrderId': order_id}, token)
        return api.post('/outbounds', {'storeOrderId': order_id, 'note': '출고 생성'}, token)['outboundId']

    def picking_complete(outbound_id, manager):
        token = ctx['token'][manager]
        api.patch('/outbounds/%d/picking/start' % outbound_id, None, token)
        detail = api.get('/outbounds/%d/details' % outbound_id, token)
        lines = [{'outboundLineId': it['outboundLineId'], 'pickedQuantity': it.get('allocatedQuantity')
                  or it.get('requestedQuantity')} for it in detail['items']]
        api.patch('/outbounds/%d/picking/complete' % outbound_id, {'lines': lines}, token)

    seoul, busan = '서울 물류센터', '부산 물류센터'

    # 1) 강남점: 배송 완료까지
    o1 = order('owner01', '강남점', seoul, [(0, 10), (1, 8), (2, 6)], '강남점 정기 주문')
    approve_assign(o1, seoul)
    ob1 = allocate_and_create(o1, 'manager01')
    picking_complete(ob1, 'manager01')
    api.patch('/outbounds/%d/ship' % ob1, None, ctx['token']['manager01'])
    api.patch('/outbounds/%d/deliver' % ob1, None, ctx['token']['manager01'])
    log('  지점 발주1: 강남점 배송 완료')

    # 2) 홍대점: 피킹 완료(PICKED)
    o2 = order('owner02', '홍대점', seoul, [(3, 12), (4, 5)], '홍대점 신상품 주문')
    approve_assign(o2, seoul)
    ob2 = allocate_and_create(o2, 'manager01')
    picking_complete(ob2, 'manager01')
    log('  지점 발주2: 홍대점 피킹 완료')

    # 3) 해운대점: 출고 생성(READY)
    o3 = order('owner03', '해운대점', busan, [(1, 7), (2, 9)], '해운대점 주문')
    approve_assign(o3, busan)
    allocate_and_create(o3, 'manager02')
    log('  지점 발주3: 해운대점 출고 대기')

    # 4) 서면점: 승인·배정만, 재고 할당 전
    o4 = order('owner04', '서면점', busan, [(3, 6)], '서면점 주문')
    approve_assign(o4, busan)
    log('  지점 발주4: 서면점 배정됨')

    # 5) 요청됨, 반려, 취소
    order('owner01', '강남점', seoul, [(5, 4)], '강남점 추가 주문 (승인 대기)')
    o6 = order('owner02', '홍대점', seoul, [(6, 50)], '홍대점 대량 주문')
    api.patch('/orders/%d/reject' % o6, {'reason': '이번 주 재고 부족으로 반려'}, hq)
    o7 = order('owner03', '해운대점', busan, [(4, 3)], '해운대점 주문 (취소)')
    api.patch('/orders/%d/cancel' % o7, {'reason': '주문 착오'}, ctx['token']['owner03'])
    log('  지점 발주5~7: 승인 대기, 반려, 취소')


def seed_adjustments(api, ctx):
    token = ctx['token']['manager01']
    lots = items_of(api.get('/inventory/by-lot', token))
    target = [row for row in lots if row.get('onHandQuantity', 0) > 5]
    if not target:
        return
    row = target[0]
    before = row['onHandQuantity']
    api.post('/inventory/adjustments', {'inventoryLotId': row['inventoryLotId'], 'beforeQuantity': before,
                                        'afterQuantity': before - 2, 'reason': '실사 결과 파손 2개 확인'}, token)
    log('  재고 조정 1건')



# --------------------------------------------------------------------------------------
# 대량 데이터 (--bulk): 업무 화면 확인용으로 상품·SKU·발주·입고·지점 발주·출고를 많이 만든다
# --------------------------------------------------------------------------------------
BULK_STORES = [
    {'storeName': '판교점', 'address': '경기도 성남시 분당구 판교역로 235', 'contactName': '판교점장', 'contactNumber': '031-5000-0001',
     'latitude': 37.3947, 'longitude': 127.1112, 'region': 'seoul', 'owner': ('owner05', '윤판교', '010-3333-0001')},
    {'storeName': '인천점', 'address': '인천광역시 남동구 인주대로 593', 'contactName': '인천점장', 'contactNumber': '032-5000-0002',
     'latitude': 37.4486, 'longitude': 126.7033, 'region': 'seoul', 'owner': ('owner06', '송인천', '010-3333-0002')},
    {'storeName': '대구점', 'address': '대구광역시 중구 동성로 2길 81', 'contactName': '대구점장', 'contactNumber': '053-5000-0003',
     'latitude': 35.8714, 'longitude': 128.5950, 'region': 'busan', 'owner': ('owner07', '권대구', '010-3333-0003')},
    {'storeName': '울산점', 'address': '울산광역시 남구 삼산로 282', 'contactName': '울산점장', 'contactNumber': '052-5000-0004',
     'latitude': 35.5384, 'longitude': 129.3394, 'region': 'busan', 'owner': ('owner08', '조울산', '010-3333-0004')},
]

# (브랜드, 카테고리, 상품명, 설명)
BULK_PRODUCTS = [
    ('YNX', 'RKT', '아스트록스 77', '공격형 배드민턴 라켓'), ('YNX', 'RKT', '듀오라 10', '밸런스형 배드민턴 라켓'),
    ('YNX', 'RKT', '나노플레어 1000', '스피드형 배드민턴 라켓'), ('VCT', 'RKT', '쓰러스터 K', '공격형 배드민턴 라켓'),
    ('VCT', 'RKT', '자이언트 시리즈 5', '올라운드 배드민턴 라켓'), ('VCT', 'RKT', '헥사 에어', '경량 배드민턴 라켓'),
    ('LNG', 'RKT', '터보차저 90', '헤드 헤비 배드민턴 라켓'), ('LNG', 'RKT', '에어스트림 N7', '경량 배드민턴 라켓'),
    ('APX', 'RKT', '프로 카본 8', '입문·중급용 카본 라켓'), ('TCH', 'RKT', '엘리트 X', '공격형 배드민턴 라켓'),
    ('YNX', 'SHC', '에어로세나 AS-30', '대회용 셔틀콕 12개입'), ('YNX', 'SHC', '에어로클럽 3000', '연습용 셔틀콕 12개입'),
    ('VCT', 'SHC', '골드 No.3', '중급 셔틀콕 12개입'), ('LNG', 'SHC', 'A+ 300', '연습용 셔틀콕 12개입'),
    ('APX', 'SHC', '트레이닝 500', '입문용 셔틀콕 12개입'),
    ('YNX', 'SHO', '파워쿠션 엘리트', '쿠션 강화 배드민턴 신발'), ('VCT', 'SHO', 'A970 ACE', '안정성 중심 신발'),
    ('LNG', 'SHO', '소닉 부스트', '반발력 강화 신발'), ('TCH', 'SHO', '코트 라이트', '경량 코트 신발'),
    ('APX', 'SHO', '스탠스 프로', '입문용 코트 신발'), ('YNX', 'SHO', '에클립션 Z', '프리미엄 코트 신발'),
    ('TRI', 'APR', '팀 반바지', '경기용 반바지'), ('TRI', 'APR', '트레이닝 후드', '트레이닝 후드 집업'),
    ('YNX', 'APR', '경기복 세트', '상하의 세트'), ('VCT', 'APR', '쿨 티셔츠', '기능성 반팔 티셔츠'),
    ('LNG', 'APR', '바람막이 자켓', '경량 바람막이'), ('APX', 'APR', '스포츠 양말 3족', '쿠션 양말 3족'),
    ('YNX', 'BAG', '프로 라켓백 9R', '라켓 9자루 수납 가방'), ('VCT', 'BAG', '토너먼트 백', '대회용 라켓 가방'),
    ('LNG', 'BAG', '미니 보스턴백', '소형 스포츠 가방'), ('APX', 'BAG', '신발 파우치', '신발 수납 파우치'),
    ('TRI', 'BAG', '스포츠 더플백', '대용량 더플백'),
    ('YNX', 'STR', '엑스보 BG80', '반발력 스트링'), ('VCT', 'STR', 'VS-850 스트링', '컨트롤 스트링'),
    ('LNG', 'STR', 'No.1 스트링', '범용 스트링'), ('APX', 'STR', '스피드 스트링', '스피드형 스트링'),
    ('TCH', 'STR', '컨트롤 스트링', '내구성 스트링'),
    ('YNX', 'GRP', '슈퍼그랩 3개입', '오버 그립 3개입'), ('VCT', 'GRP', 'GR262 베이스 그립', '베이스 그립'),
    ('LNG', 'GRP', 'GP10 타월그립', '타월 그립'), ('APX', 'GRP', '프로 오버그립 12개입', '오버 그립 12개입'),
    ('YNX', 'PRT', '손목 보호대', '손목 보호대'), ('VCT', 'PRT', '무릎 보호대', '무릎 보호대'),
    ('LNG', 'PRT', '발목 보호대', '발목 보호대'), ('APX', 'PRT', '팔꿈치 보호대', '팔꿈치 보호대'),
    ('TRI', 'PRT', '헤어밴드', '스포츠 헤어밴드'),
]

CATEGORY_PRICE = {'RKT': 80000, 'SHC': 22000, 'SHO': 75000, 'APR': 30000, 'BAG': 45000, 'STR': 8000, 'GRP': 4000,
                  'PRT': 12000}


def seed_bulk_masters(api, ctx, rng):
    hq = ctx['hq']
    existing_names = {p['productName'] for p in items_of(api.get('/products', hq))}

    # 창고별 B동(ZONE) + 랙 8개 추가: 대량 입고를 받을 수 있게 창고 전체 용량부터 늘린다
    for w in WAREHOUSES:
        wid = ctx['warehouse'][w['warehouseName']]
        pre = w['prefix']
        big = pre == 'SEL'
        api.patch('/warehouses/%d' % wid, {'totalCapacity': 300000 if big else 200000}, hq)
        by_code = {s['sectionCode']: s for s in items_of(api.get('/warehouses/%d/sections' % wid, hq))}
        zone_code = '%s-B' % pre
        if zone_code in by_code:
            zone_id = by_code[zone_code]['sectionId']
        else:
            zone_id = api.post('/warehouses/sections', {
                'warehouseId': wid, 'sectionCode': zone_code, 'sectionName': 'B동', 'sectionType': 'ZONE',
                'capacity': 160000 if big else 120000}, hq)['sectionId']
        for n in range(1, 9):
            code = '%s-B-%02d' % (pre, n)
            if code not in by_code:
                api.post('/warehouses/sections', {
                    'warehouseId': wid, 'parentSectionId': zone_id, 'sectionCode': code,
                    'sectionName': 'B동 %d번 랙' % n, 'sectionType': 'RACK',
                    'capacity': 20000 if big else 15000}, hq)
        rows = items_of(api.get('/warehouses/%d/sections' % wid, hq))
        ctx['sections'][wid]['racks'] = [s['sectionId'] for s in rows if s['sectionType'] == 'RACK']
    log('  창고 B동·랙 추가 완료')

    # 지점·점주 추가
    existing = {s['storeName']: s for s in items_of(api.get('/stores', hq))}
    users = {u['loginId']: u for u in items_of(api.get('/users', hq))}
    for st in BULK_STORES:
        if st['storeName'] in existing:
            store_id = existing[st['storeName']]['storeId']
        else:
            body = {k: v for k, v in st.items() if k not in ('region', 'owner')}
            store_id = api.post('/stores', body, hq)['storeId']
            log('  지점 등록: %s' % st['storeName'])
        ctx['store'][st['storeName']] = store_id
        login_id, name, phone = st['owner']
        if login_id not in users:
            api.post('/auth/signup', {'loginId': login_id, 'password': pw(login_id), 'name': name,
                                      'email': '%s@example.com' % login_id.replace('_', '.'), 'phone': phone,
                                      'role': 'STORE_OWNER'}, token='')
            users[login_id] = [u for u in items_of(api.get('/users', hq)) if u['loginId'] == login_id][0]
        if users[login_id].get('status') != 'ACTIVE':
            api.patch('/users/%d' % users[login_id]['userId'], {'status': 'ACTIVE', 'storeId': store_id}, hq)
        ctx['token'][login_id] = api.login(login_id, pw(login_id))
        ctx['owner_of'][st['storeName']] = login_id
        ctx['region_of'][st['storeName']] = st['region']

    # 상품·SKU·공급처 매핑
    brands = {b['brandCode']: b['brandId'] for b in items_of(api.get('/products/brands', hq)) if b.get('brandCode')}
    categories = {c['categoryCode']: c['categoryId'] for c in items_of(api.get('/products/categories', hq))}
    groups_cache = {}
    created_products = created_skus = 0
    for brand, cat, name, desc in BULK_PRODUCTS:
        if name in existing_names:
            continue
        if cat not in groups_cache:
            groups_cache[cat] = items_of(api.get('/products/categories/%d/option-groups' % categories[cat], hq))
        groups = groups_cache[cat]
        spec_ids = [g['values'][rng.randrange(len(g['values']))]['optionValueId']
                    for g in groups if g['optionType'] == 'SPEC' and g['values']]
        sku_groups = [g for g in groups if g['optionType'] == 'SKU' and g['values']]
        purchase = int(CATEGORY_PRICE[cat] * rng.uniform(0.7, 1.6) / 100) * 100
        supply = int(purchase * 1.4 / 100) * 100
        try:
            product = api.post('/products', {'brandId': brands[brand], 'categoryId': categories[cat],
                                             'productName': name, 'description': desc,
                                             'specOptionValueIds': spec_ids}, hq)
        except RuntimeError as e:
            log('  상품 건너뜀(%s): %s' % (name, e))
            continue
        created_products += 1
        for i in range(rng.choice([2, 2, 3, 3, 4])):
            option_ids = [g['values'][(i * (j + 1) + j) % len(g['values'])]['optionValueId']
                          for j, g in enumerate(sku_groups)]
            try:
                sku = api.post('/products/skus', {
                    'productId': product['productId'], 'currentPurchasePrice': purchase,
                    'currentSupplyPrice': supply, 'unit': 'EA', 'safetyStockQuantity': rng.choice([10, 20, 30, 50]),
                    'optionValueIds': option_ids}, hq)
            except RuntimeError as e:
                log('  SKU 건너뜀(%s #%d): %s' % (name, i, e))
                continue
            created_skus += 1
            targets = {BRAND_SUPPLIER[brand], 'SUP-004'}
            if rng.random() < 0.25:  # 일부 SKU는 다른 브랜드 공급처에서도 들여온다
                targets.add(rng.choice(['SUP-001', 'SUP-002', 'SUP-003']))
            for code in targets:
                api.post('/products/skus/%d/suppliers' % sku['skuId'], {'supplierId': ctx['supplier'][code]}, hq)
    log('  상품 %d개, SKU %d개 추가' % (created_products, created_skus))


def refresh_skus(api, ctx):
    """발주 확정에서 브랜드 공급처를 고르기 위해 전체 SKU를 서버에서 다시 읽는다."""
    skus = []
    for row in items_of(api.get('/products/skus', ctx['hq'])):
        brand = (row.get('skuCode') or '').split('-')[0]
        if brand in BRAND_SUPPLIER:
            skus.append({'skuId': row['skuId'], 'brand': brand})
    ctx['skus'] = skus


def seed_bulk_purchases(api, ctx, rng, count):
    hq = ctx['hq']
    refresh_skus(api, ctx)
    skus = [s['skuId'] for s in ctx['skus']]
    plan = (['full'] * 9 + ['partial'] * 3 + ['issued'] * 3 + ['inspecting'] * 2 + ['requested'] * 2
            + ['rejected', 'canceled'])
    done = 0
    for n in range(count):
        kind = plan[n % len(plan)]
        warehouse = '서울 물류센터' if n % 2 == 0 else '부산 물류센터'
        manager = 'manager01' if warehouse.startswith('서울') else 'manager02'
        lines = [(sku, rng.choice([20, 30, 40, 50, 60, 80, 100])) for sku in rng.sample(skus, rng.randint(4, 9))]
        note = '%s 대량 발주 #%d (%s)' % (warehouse[:2], n + 1, kind)
        try:
            po = request_order(api, ctx, manager, warehouse, lines, note)
            if kind == 'rejected':
                api.patch('/purchase-orders/%d/reject' % po, {'reason': '발주 수량 재검토 필요'}, hq)
            elif kind != 'requested':
                orders = confirm_order(api, ctx, po, prefer_brand_supplier=rng.random() < 0.7)
                if kind == 'canceled':
                    api.patch('/purchase-orders/%d/cancel' % po, {'reason': '공급처 납기 불가'}, hq)
                elif kind == 'full':
                    for so in orders:
                        receive(api, ctx, manager, warehouse, so['supplierOrderId'])
                elif kind == 'partial':
                    for i, so in enumerate(orders):
                        receive(api, ctx, manager, warehouse, so['supplierOrderId'],
                                ratio=rng.choice([0.3, 0.5, 0.7]) if i % 2 == 0 else 1.0)
                elif kind == 'inspecting':
                    receive(api, ctx, manager, warehouse, orders[0]['supplierOrderId'], complete=False)
            done += 1
        except RuntimeError as e:
            log('  발주 #%d(%s) 실패: %s' % (n + 1, kind, e))
    log('  대량 발주 %d/%d건 처리' % (done, count))


def seed_bulk_store_orders(api, ctx, rng, count):
    hq = ctx['hq']
    stores = list(ctx['store'].keys())
    region_of = {'강남점': 'seoul', '홍대점': 'seoul', '해운대점': 'busan', '서면점': 'busan'}
    region_of.update(ctx['region_of'])
    owner_of = {'강남점': 'owner01', '홍대점': 'owner02', '해운대점': 'owner03',
                '서면점': 'owner04'}
    owner_of.update(ctx['owner_of'])

    pools = {}
    for name, wid in ctx['warehouse'].items():
        rows = items_of(api.get('/inventory?warehouseId=%d' % wid, hq))
        pools[name] = {r['skuId']: r['availableQuantity'] for r in rows if r.get('availableQuantity', 0) >= 10}

    plan = (['delivered'] * 6 + ['shipped'] * 2 + ['picked'] * 2 + ['picking'] * 2 + ['ready'] * 2
            + ['assigned'] * 2 + ['requested'] * 2 + ['rejected', 'canceled', 'on_hold'])
    done = 0
    for n in range(count):
        kind = plan[n % len(plan)]
        store = stores[n % len(stores)]
        warehouse = '서울 물류센터' if region_of.get(store) == 'seoul' else '부산 물류센터'
        manager = 'manager01' if warehouse.startswith('서울') else 'manager02'
        owner = owner_of[store]
        pool = pools[warehouse]
        candidates = [sku for sku, qty in pool.items() if qty >= 10]
        if len(candidates) < 2:
            log('  %s 재고가 부족해 건너뜁니다.' % warehouse)
            continue
        picks = rng.sample(candidates, min(len(candidates), rng.randint(1, 5)))
        lines = [{'skuId': sku, 'requestedQuantity': rng.randint(1, max(1, min(pool[sku] // 3, 20)))}
                 for sku in picks]
        try:
            oid = api.post('/orders', {
                'storeId': ctx['store'][store],
                'requestedDeliveryAt': (datetime.now() + timedelta(days=rng.randint(2, 10))).strftime('%Y-%m-%dT%H:%M:%S'),
                'note': '%s 정기 주문 #%d' % (store, n + 1), 'lines': lines}, ctx['token'][owner])['storeOrderId']
            wm = ctx['token'][manager]
            if kind == 'requested':
                pass
            elif kind == 'rejected':
                api.patch('/orders/%d/reject' % oid, {'reason': '재고 계획상 이번 주 공급이 어렵습니다.'}, hq)
            elif kind == 'canceled':
                api.patch('/orders/%d/cancel' % oid, {'reason': '주문 내용 변경'}, ctx['token'][owner])
            else:
                api.patch('/orders/%d/approve' % oid, None, hq)
                api.post('/orders/assign', {'storeOrderId': oid, 'warehouseId': ctx['warehouse'][warehouse],
                                            'reason': '권역 물류센터 배정'}, hq)
                if kind == 'on_hold':
                    api.patch('/orders/%d/hold' % oid, {'reason': '재고 실사 중'}, wm)
                elif kind != 'assigned':
                    api.post('/allocations', {'storeOrderId': oid}, wm)
                    for line in lines:  # 로컬 재고 풀에서 차감
                        pool[line['skuId']] = pool.get(line['skuId'], 0) - line['requestedQuantity']
                    ob = api.post('/outbounds', {'storeOrderId': oid, 'note': '출고 작업 생성'}, wm)['outboundId']
                    if kind != 'ready':
                        api.patch('/outbounds/%d/picking/start' % ob, None, wm)
                    if kind in ('picked', 'shipped', 'delivered'):
                        detail = api.get('/outbounds/%d/details' % ob, wm)
                        picked = [{'outboundLineId': it['outboundLineId'],
                                   'pickedQuantity': it.get('allocatedQuantity') or it.get('requestedQuantity')}
                                  for it in detail['items']]
                        api.patch('/outbounds/%d/picking/complete' % ob, {'lines': picked}, wm)
                    if kind in ('shipped', 'delivered'):
                        api.patch('/outbounds/%d/ship' % ob, None, wm)
                    if kind == 'delivered':
                        api.patch('/outbounds/%d/deliver' % ob, None, wm)
            done += 1
        except RuntimeError as e:
            log('  지점 발주 #%d(%s) 실패: %s' % (n + 1, kind, e))
    log('  대량 지점 발주 %d/%d건 처리' % (done, count))


def main():
    parser = argparse.ArgumentParser(description='WMS 목데이터 시드')
    parser.add_argument('--base-url', default='http://localhost:8080')
    parser.add_argument('--admin-id', default=read_env('WMS_ADMIN_LOGIN_ID') or 'admin')
    parser.add_argument('--admin-password', default=read_env('WMS_ADMIN_PASSWORD'))
    parser.add_argument('--masters-only', action='store_true', help='업무 흐름 데이터는 만들지 않는다')
    parser.add_argument('--bulk', action='store_true',
                        help='업무 확인용 대량 데이터(상품·SKU·발주·입고·지점 발주·출고)를 추가한다')
    parser.add_argument('--purchase-orders', type=int, default=40, help='--bulk일 때 만들 창고 발주 수')
    parser.add_argument('--store-orders', type=int, default=60, help='--bulk일 때 만들 지점 발주 수')
    parser.add_argument('--seed', type=int, default=20261015, help='무작위 시드(같은 값이면 같은 데이터)')
    args = parser.parse_args()
    if not args.admin_password:
        sys.exit('본사 관리자 비밀번호가 필요합니다(.env의 WMS_ADMIN_PASSWORD 또는 --admin-password).')

    api = Api(args.base_url)
    ctx = {'lot_seq': 0, 'token': {}, 'owner_of': {}, 'region_of': {}}
    ctx['hq'] = api.login(args.admin_id, args.admin_password)
    log('본사 관리자 로그인 완료')

    log('[1/4] 마스터 데이터')
    seed_masters(api, ctx)
    for login_id, *_ in USERS:
        ctx['token'][login_id] = api.login(login_id, pw(login_id))

    if args.masters_only:
        return
    if args.bulk:
        import random
        rng = random.Random(args.seed)
        ctx['lot_seq'] = 1000  # 기본 시드와 로트 번호가 겹치지 않게 이어서 쓴다
        log('[bulk 1/3] 상품·SKU·지점 추가')
        seed_bulk_masters(api, ctx, rng)
        log('[bulk 2/3] 창고 발주·입고')
        seed_bulk_purchases(api, ctx, rng, args.purchase_orders)
        log('[bulk 3/3] 지점 발주·출고')
        seed_bulk_store_orders(api, ctx, rng, args.store_orders)
        log('대량 데이터 추가 완료')
        return
    if items_of(api.get('/purchase-orders', ctx['hq'])):
        log('이미 발주가 있어 업무 흐름 데이터는 건너뜁니다. (처음부터 만들려면 DB를 초기화하세요)')
        return

    log('[2/4] 발주·입고 흐름')
    seed_purchase_flows(api, ctx)
    log('[3/4] 지점 발주·출고 흐름')
    seed_store_orders(api, ctx)
    log('[4/4] 재고 조정과 마무리')
    seed_adjustments(api, ctx)
    # 비활성 공급처는 매핑·발주가 끝난 뒤에 비활성화한다.
    for s in SUPPLIERS:
        if s.get('deactivate'):
            api.patch('/suppliers/%d/deactivate' % ctx['supplier'][s['supplierCode']], None, ctx['hq'])
    log('완료. 로그인: 아이디 / 비밀번호는 아이디 + ! (예: %s / %s)  계정: %s'
        % (USERS[0][0], pw(USERS[0][0]), ', '.join(u[0] for u in USERS)))


if __name__ == '__main__':
    main()
