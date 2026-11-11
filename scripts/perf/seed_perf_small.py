#!/usr/bin/env python3
"""성능 테스트용 소규모 데이터 시드 (docs/scenario/performance.md §3 소규모 단계).

기본값은 SKU 1천, 재고 이력(InventoryTransaction) 1만 건이다. seed_mock_data.py와 같이
서버 API만 호출하고 DB에 직접 INSERT 하지 않는다. 마스터 데이터(창고·구역·지점·공급처·기본 상품)는
seed_mock_data.py의 함수를 그대로 쓴다.

사용법 (서버가 http://localhost:8080 에서 실행 중이고 본사 관리자가 있어야 한다)
    python scripts/perf/seed_perf_small.py
    python scripts/perf/seed_perf_small.py --skus 500 --transactions 5000 --load-users 20

만들어지는 것
    1) 기본 마스터 + 지점·점주 추가(seed_mock_data의 --bulk 마스터) + 합성 상품·SKU(--skus까지)
    2) 부하용 사용자: perfwm01~(창고 관리자), perfowner01~(점주). 비밀번호는 아이디 + '!'
    3) 전 SKU 대상 창고 발주·입고를 반복해 재고 이력을 --transactions까지 채운다
    4) 지점 발주·출고 흐름 --store-orders건(S1·S2의 주문·할당·출고 조회 대상)

재고 이력은 입고 외에 지점 발주 출고에서도 쌓인다. 그래서 입고로 목표의 90%를 먼저 채우고,
지점 발주를 만든 뒤 나머지를 입고로 채워 목표에 맞춘다. 같은 --seed면 같은 데이터가 만들어진다.
로트 번호는 서버에 있는 로트 수에서 이어 붙이므로 다시 실행해도 겹치지 않는다.
처음부터 다시 만들려면 DB를 초기화한다: docker-compose down -v && docker-compose up -d --build
"""
import argparse
import math
import os
import random
import sys
import time

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..'))
import seed_mock_data as base  # noqa: E402
from seed_mock_data import items_of, log, pw  # noqa: E402

RACK_MIN_CAPACITY = 15000  # A동의 작은 랙(5000)은 대량 입고를 받지 못해 제외한다
LOAD_USERS = (
    # (아이디 접두사, 역할, 소속 종류, 연락처 접두사)
    ('perfwm', 'WAREHOUSE_MANAGER', 'warehouse', '010-9100'),
    ('perfowner', 'STORE_OWNER', 'store', '010-9200'),
)


class PagedApi(base.Api):
    """목록 API는 기본 20건만 돌려주므로, 기존 시드 함수가 전체를 읽도록 모든 페이지를 모아서 돌려준다."""
    PAGE_SIZE = 100  # 서버가 허용하는 최대 크기

    def get(self, path, token=None):
        first = path if 'size=' in path else '%s%ssize=%d' % (path, '&' if '?' in path else '?', self.PAGE_SIZE)
        data = super().get(first, token)
        if not (isinstance(data, dict) and data.get('pageInfo')):
            return data
        for page in range(2, data['pageInfo']['totalPages'] + 1):
            data['items'] += super().get('%s&page=%d' % (first, page), token)['items']
        return data

    def total(self, path, token):
        """페이징 목록의 전체 건수. 한 건만 읽어 pageInfo로 센다."""
        sep = '&' if '?' in path else '?'
        return super().get('%s%ssize=1' % (path, sep), token)['pageInfo']['totalElements']


# --------------------------------------------------------------------------------------
# 합성 상품·SKU
# --------------------------------------------------------------------------------------
def seed_products(api, ctx, rng, sku_target):
    hq = ctx['hq']
    sku_count = api.total('/products/skus', hq)
    if sku_count >= sku_target:
        log('  SKU가 이미 %d개라 상품을 추가하지 않습니다.' % sku_count)
        return
    brands = {b['brandCode']: b['brandId'] for b in items_of(api.get('/products/brands', hq)) if b.get('brandCode')}
    categories = {c['categoryCode']: c['categoryId'] for c in items_of(api.get('/products/categories', hq))}
    brand_codes, category_codes = list(base.BRAND_SUPPLIER), list(base.CATEGORY_PRICE)
    groups_cache = {}
    serial = api.total('/products', hq)  # 다시 실행해도 상품명이 겹치지 않게 이어서 센다
    failures = 0
    while sku_count < sku_target:
        serial += 1
        brand, cat = rng.choice(brand_codes), rng.choice(category_codes)
        if cat not in groups_cache:
            groups_cache[cat] = items_of(api.get('/products/categories/%d/option-groups' % categories[cat], hq))
        groups = groups_cache[cat]
        spec_ids = [g['values'][rng.randrange(len(g['values']))]['optionValueId']
                    for g in groups if g['optionType'] == 'SPEC' and g['values']]
        sku_groups = [g for g in groups if g['optionType'] == 'SKU' and g['values']]
        purchase = int(base.CATEGORY_PRICE[cat] * rng.uniform(0.7, 1.6) / 100) * 100
        supply = int(purchase * 1.4 / 100) * 100
        try:
            product = api.post('/products', {'brandId': brands[brand], 'categoryId': categories[cat],
                                             'productName': 'PERF-%s-%s-%05d' % (brand, cat, serial),
                                             'description': '성능 테스트용 합성 상품', 'specOptionValueIds': spec_ids}, hq)
        except RuntimeError as e:
            failures += 1
            log('  상품 건너뜀(%05d): %s' % (serial, e))
            if failures >= 20:
                sys.exit('상품 등록이 계속 실패해 중단합니다.')
            continue
        for i in range(rng.choice([2, 3, 3, 4])):
            if sku_count >= sku_target:
                break
            option_ids = [g['values'][(i * (j + 1) + j) % len(g['values'])]['optionValueId']
                          for j, g in enumerate(sku_groups)]
            try:
                sku = api.post('/products/skus', {
                    'productId': product['productId'], 'currentPurchasePrice': purchase,
                    'currentSupplyPrice': supply, 'unit': 'EA', 'safetyStockQuantity': rng.choice([10, 20, 30, 50]),
                    'optionValueIds': option_ids}, hq)
            except RuntimeError as e:
                log('  SKU 건너뜀(%05d #%d): %s' % (serial, i, e))
                continue
            for code in {base.BRAND_SUPPLIER[brand], 'SUP-004'}:
                api.post('/products/skus/%d/suppliers' % sku['skuId'], {'supplierId': ctx['supplier'][code]}, hq)
            sku_count += 1
            if sku_count % 100 == 0:
                log('  SKU %d/%d' % (sku_count, sku_target))
    log('  SKU %d개 준비 완료' % sku_count)


# --------------------------------------------------------------------------------------
# 부하용 사용자
# --------------------------------------------------------------------------------------
def seed_load_users(api, ctx, count):
    hq = ctx['hq']
    users = {u['loginId']: u for u in items_of(api.get('/users', hq))}
    targets = {'warehouse': list(ctx['warehouse'].values()), 'store': list(ctx['store'].values())}
    created = 0
    for n in range(1, count + 1):
        for prefix, role, kind, phone in LOAD_USERS:
            login_id = '%s%02d' % (prefix, n)
            if login_id not in users:
                api.post('/auth/signup', {'loginId': login_id, 'password': pw(login_id), 'name': '%s%02d' % (prefix, n),
                                          'email': '%s@example.com' % login_id, 'phone': '%s-%04d' % (phone, n),
                                          'role': role}, token='')
                users[login_id] = [u for u in items_of(api.get('/users', hq)) if u['loginId'] == login_id][0]
                created += 1
            if users[login_id].get('status') != 'ACTIVE':
                owner_ids = targets[kind]
                key = 'warehouseId' if kind == 'warehouse' else 'storeId'
                api.patch('/users/%d' % users[login_id]['userId'],
                          {'status': 'ACTIVE', key: owner_ids[(n - 1) % len(owner_ids)]}, hq)
    log('  부하용 사용자 %d명 추가 (창고 관리자·점주 각 %d명까지)' % (created, count))


# --------------------------------------------------------------------------------------
# 창고 발주·입고 (재고 이력을 쌓는 주된 경로)
# --------------------------------------------------------------------------------------
def sku_stream(sku_ids, rng):
    """전 SKU를 한 바퀴씩 섞어서 내보내 SKU마다 입고 횟수가 고르게 되도록 한다."""
    while True:
        batch = sku_ids[:]
        rng.shuffle(batch)
        yield from batch


def take(stream, size):
    """한 발주서 안에서는 SKU가 겹치면 안 되므로(발주 라인 UNIQUE) 중복을 건너뛴다."""
    picked = []
    while len(picked) < size:
        sku = next(stream)
        if sku not in picked:
            picked.append(sku)
    return picked


def use_large_racks(api, ctx):
    hq = ctx['hq']
    for wid in ctx['warehouse'].values():
        rows = items_of(api.get('/warehouses/%d/sections' % wid, hq))
        racks = [s['sectionId'] for s in rows
                 if s['sectionType'] == 'RACK' and (s.get('capacity') or 0) >= RACK_MIN_CAPACITY]
        if racks:
            ctx['sections'][wid]['racks'] = racks


def seed_inbound(api, ctx, rng, tx_target, lines_per_po, stream):
    hq = ctx['hq']
    tx = api.total('/inventory/transactions', hq)
    if tx >= tx_target:
        return
    start_tx, sent_lines, failures, po_n = tx, 0, 0, 0
    while tx < tx_target:
        # 입고 라인당 이력이 몇 건 생기는지(불량 구역 분리 등으로 1건이 넘을 수 있다)는 보내 보며 보정한다.
        per_line = (tx - start_tx) / sent_lines if sent_lines and tx > start_tx else None
        size = lines_per_po if per_line is None else min(lines_per_po, math.ceil((tx_target - tx) / per_line))
        warehouse = '서울 물류센터' if po_n % 2 == 0 else '부산 물류센터'
        manager = 'manager01' if po_n % 2 == 0 else 'manager02'
        po_n += 1
        lines = [(sku, rng.randint(10, 20)) for sku in take(stream, size)]
        try:
            po = base.request_order(api, ctx, manager, warehouse, lines, '성능 테스트 입고 #%d' % po_n)
            for so in base.confirm_order(api, ctx, po, prefer_brand_supplier=rng.random() < 0.7):
                base.receive(api, ctx, manager, warehouse, so['supplierOrderId'])
            sent_lines += size
            failures = 0
        except RuntimeError as e:
            failures += 1
            log('  입고 #%d 실패: %s' % (po_n, e))
            if failures >= 5:
                sys.exit('입고가 연속 5번 실패해 중단합니다.')
        tx = api.total('/inventory/transactions', hq)
        log('  입고 #%d 처리 -> 재고 이력 %d/%d' % (po_n, tx, tx_target))


def main():
    parser = argparse.ArgumentParser(description='성능 테스트용 소규모 데이터 시드')
    parser.add_argument('--base-url', default='http://localhost:8080')
    parser.add_argument('--admin-id', default=base.read_env('WMS_ADMIN_LOGIN_ID') or 'admin')
    parser.add_argument('--admin-password', default=base.read_env('WMS_ADMIN_PASSWORD'))
    parser.add_argument('--skus', type=int, default=1000, help='만들 SKU 수(소규모 단계: 1천)')
    parser.add_argument('--transactions', type=int, default=10000, help='채울 재고 이력 수(소규모 단계: 1만)')
    parser.add_argument('--lines-per-po', type=int, default=100, help='창고 발주서 한 장의 라인 수')
    parser.add_argument('--load-users', type=int, default=30, help='창고 관리자·점주 부하용 계정을 각각 몇 명까지 만들지')
    parser.add_argument('--store-orders', type=int, default=200, help='지점 발주·출고 흐름 건수')
    parser.add_argument('--seed', type=int, default=20261015, help='무작위 시드(같은 값이면 같은 데이터)')
    args = parser.parse_args()
    if not args.admin_password:
        sys.exit('본사 관리자 비밀번호가 필요합니다(.env의 WMS_ADMIN_PASSWORD 또는 --admin-password).')

    started = time.time()
    rng = random.Random(args.seed)
    api = PagedApi(args.base_url)
    ctx = {'lot_seq': 0, 'token': {}, 'owner_of': {}, 'region_of': {}}
    ctx['hq'] = api.login(args.admin_id, args.admin_password)
    log('본사 관리자 로그인 완료')

    log('[1/5] 기본 마스터 + 지점·점주 추가')
    base.seed_masters(api, ctx)
    for login_id, *_ in base.USERS:
        ctx['token'][login_id] = api.login(login_id, pw(login_id))
    ctx['lot_seq'] = 100000 + api.total('/lots', ctx['hq'])
    base.seed_bulk_masters(api, ctx, rng)

    log('[2/5] 합성 상품·SKU')
    seed_products(api, ctx, rng, args.skus)

    log('[3/5] 부하용 사용자')
    seed_load_users(api, ctx, args.load_users)

    log('[4/5] 창고 발주·입고 (재고 이력 목표의 90%)')
    base.refresh_skus(api, ctx)
    stream = sku_stream([s['skuId'] for s in ctx['skus']], rng)
    use_large_racks(api, ctx)
    seed_inbound(api, ctx, rng, int(args.transactions * 0.9), args.lines_per_po, stream)

    log('[5/5] 지점 발주·출고, 나머지 입고')
    base.seed_bulk_store_orders(api, ctx, rng, args.store_orders)
    seed_inbound(api, ctx, rng, args.transactions, args.lines_per_po, stream)

    hq = ctx['hq']
    log('완료 (%d초). SKU %d개, 재고 이력 %d건, 창고 발주 %d건, 지점 발주 %d건, 출고 %d건' % (
        time.time() - started, api.total('/products/skus', hq), api.total('/inventory/transactions', hq),
        api.total('/purchase-orders', hq), api.total('/orders', hq), api.total('/outbounds', hq)))
    log('부하용 계정: perfwm01~%02d / perfowner01~%02d (비밀번호는 아이디 + !)' % (args.load_users, args.load_users))


if __name__ == '__main__':
    main()
