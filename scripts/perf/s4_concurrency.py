#!/usr/bin/env python3
"""S4 동시성 정합성 실행 (docs/scenario/performance.md S4, 4-1 ~ 4-9).

서버 API만 호출한다. 경합마다 새 재고·발주를 만들고, 같은 순간에 요청 N건을 보낸 뒤
상태와 수량이 맞는지 확인한다. 모든 시나리오 끝에 DB의 데드락 누적 건수(information_schema.INNODB_METRICS
lock_deadlocks)를 시나리오별로 기록한다. k6 대신 파이썬 스레드로 보낸다(레포 스크립트가 모두 파이썬이고 별도 설치가 필요 없다).

사용법 (서버가 떠 있고 본사 관리자가 있어야 한다. 데이터가 쌓이므로 개발용 DB가 아닌 실험용 DB를 쓴다)
    python scripts/perf/s4_concurrency.py --base-url http://localhost:8081 --label after
    python scripts/perf/s4_concurrency.py --only 4-7,4-9 --rounds 10

--db-container가 있으면(기본 wms-db) docker exec로 데드락 건수를 읽는다. 없으면 그 칸은 비운다.
"""
import argparse
import io
import json
import os
import re
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.request
from collections import Counter
from contextlib import redirect_stdout

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..'))
import seed_mock_data as base  # noqa: E402
from seed_mock_data import items_of, pw  # noqa: E402

SEOUL = '서울 물류센터'


def log(msg):
    print(msg, flush=True)


class Client:
    """상태 코드와 errorCode를 함께 돌려주는 호출기. 연결 오류·타임아웃은 상태 0으로 센다."""

    def __init__(self, base_url):
        self.base = base_url.rstrip('/') + '/api/v1'

    def request(self, method, path, body=None, token=None, timeout=70):
        headers = {'Content-Type': 'application/json; charset=utf-8'}
        if token:
            headers['Authorization'] = 'Bearer ' + token
        data = json.dumps(body, ensure_ascii=False).encode('utf-8') if body is not None else None
        req = urllib.request.Request(self.base + path, data=data, method=method, headers=headers)
        try:
            with urllib.request.urlopen(req, timeout=timeout) as resp:
                payload = json.loads(resp.read().decode('utf-8') or '{}')
                return resp.status, payload.get('data'), None
        except urllib.error.HTTPError as e:
            payload = json.loads(e.read().decode('utf-8') or '{}')
            return e.code, None, payload.get('errorCode')
        except Exception as e:  # noqa: BLE001
            return 0, None, type(e).__name__

    def ok(self, method, path, body=None, token=None):
        status, data, code = self.request(method, path, body, token)
        if status >= 300 or status == 0:
            raise RuntimeError('%s %s -> %s %s' % (method, path, status, code))
        return data


def fire(calls):
    """호출(함수) 목록을 같은 순간에 시작시키고 (상태, errorCode) 목록을 입력 순서대로 돌려준다."""
    barrier = threading.Barrier(len(calls))
    results = [None] * len(calls)

    def run(i, call):
        barrier.wait()
        results[i] = call()

    threads = [threading.Thread(target=run, args=(i, c)) for i, c in enumerate(calls)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    return [(r[0], r[2]) for r in results]


def tally(results):
    counts = Counter('%s%s' % (s, '/' + c if c else '') for s, c in results)
    return dict(sorted(counts.items()))


class Env:
    def __init__(self, args):
        self.args = args
        self.client = Client(args.base_url)
        self.api = base.Api(args.base_url)
        self.ctx = {'lot_seq': args.lot_start, 'token': {}, 'warehouse': {}, 'sections': {}, 'skus': [],
                    'supplier': {}, 'store': {}, 'hq': None}
        self.sku_cursor = 0
        with redirect_stdout(io.StringIO()):
            self.ctx['hq'] = self.api.login(args.admin_id, args.admin_password)
            base.seed_masters(self.api, self.ctx)
        for login_id in ('manager01', 'owner01', 'owner02'):
            self.ctx['token'][login_id] = self.api.login(login_id, pw(login_id))
        self.hq = self.ctx['hq']
        self.manager = self.ctx['token']['manager01']
        self.owner = self.ctx['token']['owner01']
        self.warehouse_id = self.ctx['warehouse'][SEOUL]
        self.gangnam = self.ctx['store']['강남점']

    def next_sku(self):
        sku = self.ctx['skus'][self.sku_cursor % len(self.ctx['skus'])]['skuId']
        self.sku_cursor += 1
        return sku

    def stock(self, sku, qty):
        """발주 → 확정 → 입고 완료로 qty만큼 재고를 만들고 재고 행을 돌려준다."""
        po = base.request_order(self.api, self.ctx, 'manager01', SEOUL, [(sku, qty)], 'S4 재고 준비')
        orders = base.confirm_order(self.api, self.ctx, po)
        base.receive(self.api, self.ctx, 'manager01', SEOUL, orders[0]['supplierOrderId'])
        rows = items_of(self.api.get('/inventory/by-lot?skuId=%d&warehouseId=%d&size=100'
                                     % (sku, self.warehouse_id), self.manager))
        rows = [r for r in rows if r['qualityStatus'] == 'AVAILABLE']
        return max(rows, key=lambda r: r['inventoryLotId'])

    def inventory(self, inventory_lot_id, sku):
        rows = items_of(self.api.get('/inventory/by-lot?skuId=%d&warehouseId=%d&size=100'
                                     % (sku, self.warehouse_id), self.manager))
        return [r for r in rows if r['inventoryLotId'] == inventory_lot_id][0]

    def sku_total(self, sku):
        """SKU의 이 창고 재고 합계. 할당이 여러 로트로 나뉠 수 있어 합계로 확인한다."""
        rows = items_of(self.api.get('/inventory/by-lot?skuId=%d&warehouseId=%d&size=100'
                                     % (sku, self.warehouse_id), self.manager))
        return {k: sum(r[k] for r in rows) for k in ('onHandQuantity', 'allocatedQuantity', 'availableQuantity')}

    def store_order(self, sku, qty, owner=None):
        owner = owner or self.owner
        return self.api.post('/orders', {
            'storeId': self.gangnam, 'requestedDeliveryAt': '2026-12-31T10:00:00',
            'note': 'S4', 'lines': [{'skuId': sku, 'requestedQuantity': qty}]}, owner)['storeOrderId']

    def approve_assign(self, order_id):
        self.api.patch('/orders/%d/approve' % order_id, None, self.hq)
        self.api.post('/orders/assign', {'storeOrderId': order_id, 'warehouseId': self.warehouse_id,
                                         'reason': 'S4'}, self.hq)

    def order_status(self, order_id):
        return self.api.get('/orders/%d' % order_id, self.hq).get('status')


def deadlocks(args):
    if not args.db_container:
        return None
    root = base.read_env('MYSQL_ROOT_PASSWORD')
    out = subprocess.run(
        ['docker', 'exec', args.db_container, 'mysql', '-uroot', '-p' + root, '-N', '-e',
         "select count from information_schema.INNODB_METRICS where name='lock_deadlocks'"],
        capture_output=True, text=True)
    m = re.search(r'^\d+$', out.stdout, re.M)
    return int(m.group()) if m else None


# --------------------------------------------------------------------------------------
# 시나리오. 각 함수는 {'tally': 상태 집계, 'ok': bool, 'detail': 문자열}을 돌려준다.
# --------------------------------------------------------------------------------------
def s_4_1(env):
    """서로 다른 발주 20건이 같은 SKU에 수량 10씩 동시 할당 (가용 100)."""
    sku = env.next_sku()
    row = env.stock(sku, 100)
    orders = []
    for _ in range(20):
        oid = env.store_order(sku, 10)
        env.approve_assign(oid)
        orders.append(oid)
    results = fire([lambda o=o: env.client.request('POST', '/allocations', {'storeOrderId': o}, env.manager)
                    for o in orders])
    row = env.sku_total(sku)
    success = sum(1 for s, _ in results if 0 < s < 300)
    # 가용 100에 10씩이므로 정확히 10건이 성공하고, 나머지는 INSUFFICIENT_STOCK(409)이어야 한다.
    rejected = sum(1 for s, c in results if s == 409 and c == 'INSUFFICIENT_STOCK')
    ok = (row['allocatedQuantity'] <= row['onHandQuantity'] and row['availableQuantity'] >= 0
          and row['allocatedQuantity'] == 10 * success and success == 10 and rejected == 10)
    return {'tally': tally(results), 'ok': ok,
            'detail': '성공 %d건, 할당 %d / 보유 %d' % (success, row['allocatedQuantity'], row['onHandQuantity'])}


def s_4_2(env):
    """같은 발주의 할당 20건 동시 호출."""
    sku = env.next_sku()
    row = env.stock(sku, 100)
    oid = env.store_order(sku, 10)
    env.approve_assign(oid)
    results = fire([lambda: env.client.request('POST', '/allocations', {'storeOrderId': oid}, env.manager)
                    for _ in range(20)])
    row = env.sku_total(sku)
    success = sum(1 for s, _ in results if 0 < s < 300)
    return {'tally': tally(results), 'ok': success == 1 and row['allocatedQuantity'] == 10,
            'detail': '성공 %d건, 할당 %d (기대 10)' % (success, row['allocatedQuantity'])}


def s_4_3(env):
    """같은 입고의 완료 20건 동시 호출."""
    sku = env.next_sku()
    po = base.request_order(env.api, env.ctx, 'manager01', SEOUL, [(sku, 100)], 'S4 입고 완료 경합')
    so = base.confirm_order(env.api, env.ctx, po)[0]['supplierOrderId']
    inbound = base.receive(env.api, env.ctx, 'manager01', SEOUL, so, complete=False)
    results = fire([lambda: env.client.request('PATCH', '/inbounds/%d/complete' % inbound, None, env.manager)
                    for _ in range(20)])
    rows = items_of(env.api.get('/inventory/by-lot?skuId=%d&warehouseId=%d&size=100' % (sku, env.warehouse_id),
                                env.manager))
    on_hand = sum(r['onHandQuantity'] for r in rows)
    success = sum(1 for s, _ in results if 0 < s < 300)
    return {'tally': tally(results), 'ok': success == 1 and on_hand == 100,
            'detail': '성공 %d건, 재고 %d (기대 100)' % (success, on_hand)}


def s_4_4(env):
    """같은 재고의 조정 20건(같은 beforeQuantity) 동시 호출."""
    sku = env.next_sku()
    row = env.stock(sku, 100)
    before = row['onHandQuantity']
    results = fire([lambda i=i: env.client.request('POST', '/inventory/adjustments', {
        'inventoryLotId': row['inventoryLotId'], 'beforeQuantity': before, 'afterQuantity': before + 1 + i,
        'reason': 'S4 동시 조정'}, env.manager) for i in range(20)])
    after = env.inventory(row['inventoryLotId'], sku)['onHandQuantity']
    success = sum(1 for s, _ in results if 0 < s < 300)
    stale = sum(1 for s, c in results if s == 409 and c == 'STALE_QUANTITY')
    return {'tally': tally(results), 'ok': success == 1 and stale == 19 and before < after <= before + 20,
            'detail': '성공 %d건, STALE_QUANTITY %d건, 최종 %d' % (success, stale, after)}


def s_4_5(env):
    """같은 발주에 승인 5, 반려 5, 취소 5 동시 요청."""
    sku = env.next_sku()
    oid = env.store_order(sku, 1)
    owner = env.owner
    calls = []
    for _ in range(5):
        calls.append(lambda: env.client.request('PATCH', '/orders/%d/approve' % oid, None, env.hq))
        calls.append(lambda: env.client.request('PATCH', '/orders/%d/reject' % oid, {'reason': 'S4'}, env.hq))
        calls.append(lambda: env.client.request('PATCH', '/orders/%d/cancel' % oid, {'reason': 'S4'}, owner))
    results = fire(calls)
    success = sum(1 for s, _ in results if 0 < s < 300)
    status = env.order_status(oid)
    return {'tally': tally(results), 'ok': success == 1 and status in ('APPROVED', 'REJECTED', 'CANCELED'),
            'detail': '성공 %d건, 최종 상태 %s' % (success, status)}


def prepare_outbounds(env, sku, count):
    """같은 SKU 1개씩 요청하는 발주 count건을 할당·출고 생성·피킹 시작까지 만들고 (출고ID, 라인) 목록을 돌려준다."""
    outbounds = []
    for _ in range(count):
        oid = env.store_order(sku, 1)
        env.approve_assign(oid)
        env.api.post('/allocations', {'storeOrderId': oid}, env.manager)
        ob = env.api.post('/outbounds', {'storeOrderId': oid, 'note': 'S4'}, env.manager)['outboundId']
        env.api.patch('/outbounds/%d/picking/start' % ob, None, env.manager)
        detail = env.api.get('/outbounds/%d/details' % ob, env.manager)
        lines = [{'outboundLineId': it['outboundLineId'],
                  'pickedQuantity': it.get('allocatedQuantity') or it.get('requestedQuantity')}
                 for it in detail['items']]
        outbounds.append((ob, lines))
    return outbounds


def s_4_7(env):
    """같은 재고에서 출고 20건의 피킹 완료(ship)를 동시에 호출."""
    sku = env.next_sku()
    row = env.stock(sku, 100)
    outbounds = prepare_outbounds(env, sku, 20)
    results = fire([lambda ob=ob, lines=lines: env.client.request(
        'PATCH', '/outbounds/%d/picking/complete' % ob, {'lines': lines}, env.manager) for ob, lines in outbounds])
    row = env.sku_total(sku)
    success = sum(1 for s, _ in results if 0 < s < 300)
    return {'tally': tally(results), 'ok': success == 20 and row['onHandQuantity'] == 80
            and row['allocatedQuantity'] == 0,
            'detail': '성공 %d건, 보유 %d (기대 80), 할당 %d (기대 0)'
                      % (success, row['onHandQuantity'], row['allocatedQuantity'])}


def s_4_8(env):
    """같은 재고에서 피킹 완료와 재고 조정을 라운드마다 동시에 호출 (--rounds 라운드)."""
    rounds = env.args.rounds
    sku = env.next_sku()
    row = env.stock(sku, 1000)
    outbounds = prepare_outbounds(env, sku, rounds)
    total = Counter()
    for ob, lines in outbounds:
        current = env.inventory(row['inventoryLotId'], sku)['onHandQuantity']
        results = fire([
            lambda: env.client.request('PATCH', '/outbounds/%d/picking/complete' % ob, {'lines': lines}, env.manager),
            lambda: env.client.request('POST', '/inventory/adjustments', {
                'inventoryLotId': row['inventoryLotId'], 'beforeQuantity': current, 'afterQuantity': current + 1,
                'reason': 'S4 피킹 교차'}, env.manager)])
        total.update(tally(results))
    bad = {k: v for k, v in total.items() if not (k.startswith('200') or k.startswith('201') or k == '409/STALE_QUANTITY')}
    final = env.sku_total(sku)
    return {'tally': dict(sorted(total.items())), 'ok': not bad and final['allocatedQuantity'] == 0,
            'detail': '%d라운드, 비정상 응답 %s, 최종 할당 %d' % (rounds, bad or '없음', final['allocatedQuantity'])}


def s_4_9(env):
    """같은 공급처 발주서에 입고 등록 5건과 발주 취소 5건을 동시에 호출 (--rounds 라운드)."""
    rounds = env.args.rounds
    violations = 0
    total = Counter()
    sku = env.next_sku()
    for _ in range(rounds):
        po = base.request_order(env.api, env.ctx, 'manager01', SEOUL, [(sku, 10)], 'S4 입고 등록 vs 취소')
        so = base.confirm_order(env.api, env.ctx, po)[0]['supplierOrderId']
        calls = []
        for _ in range(5):
            calls.append(lambda: env.client.request('POST', '/inbounds', {
                'supplierOrderId': so, 'arrivedAt': '2026-11-16T09:00:00', 'note': 'S4'}, env.manager))
            calls.append(lambda: env.client.request('PATCH', '/purchase-orders/%d/cancel' % po,
                                                    {'reason': 'S4 동시 취소'}, env.hq))
        total.update(tally(fire(calls)))
        inbounds = items_of(env.api.get('/inbounds?supplierOrderId=%d' % so, env.manager))
        live = [i for i in inbounds if i['status'] != 'CANCELED']
        order_status = env.api.get('/purchase-orders/%d' % po, env.hq)['status']
        if order_status == 'CANCELED' and live:
            violations += 1
    return {'tally': dict(sorted(total.items())), 'ok': violations == 0,
            'detail': '%d라운드 중 취소된 발주에 입고가 남은 라운드 %d' % (rounds, violations)}


SCENARIOS = [
    ('4-1', '서로 다른 발주 20건 동시 할당(가용 100, 수량 10씩)', s_4_1),
    ('4-2', '같은 발주 할당 20건', s_4_2),
    ('4-3', '같은 입고 완료 20건', s_4_3),
    ('4-4', '같은 재고 조정 20건', s_4_4),
    ('4-5', '같은 발주 승인·반려·취소 각 5건', s_4_5),
    ('4-7', '같은 재고 피킹 완료 20건', s_4_7),
    ('4-8', '피킹 완료 vs 재고 조정 교차', s_4_8),
    ('4-9', '입고 등록 vs 발주 취소', s_4_9),
]


def main():
    parser = argparse.ArgumentParser(description='S4 동시성 정합성 실행')
    parser.add_argument('--base-url', default='http://localhost:8081')
    parser.add_argument('--admin-id', default=base.read_env('WMS_ADMIN_LOGIN_ID') or 'admin')
    parser.add_argument('--admin-password', default=base.read_env('WMS_ADMIN_PASSWORD'))
    parser.add_argument('--db-container', default='wms-db', help='데드락 건수를 읽을 MySQL 컨테이너(빈 값이면 생략)')
    parser.add_argument('--only', default='', help='실행할 시나리오 번호(쉼표 구분). 예: 4-7,4-9')
    parser.add_argument('--rounds', type=int, default=20, help='4-8, 4-9의 반복 라운드 수')
    parser.add_argument('--repeat', type=int, default=1, help='시나리오 전체 반복 횟수')
    parser.add_argument('--lot-start', type=int, default=int(time.time()) % 100000 * 1000,
                        help='로트 번호 시작값(같은 DB에서 다시 실행해도 겹치지 않게 시각에서 만든다)')
    parser.add_argument('--label', default='run')
    parser.add_argument('--out', help='결과를 JSON으로 저장할 경로')
    args = parser.parse_args()
    if not args.admin_password:
        sys.exit('본사 관리자 비밀번호가 필요합니다(.env의 WMS_ADMIN_PASSWORD 또는 --admin-password).')

    only = {s.strip() for s in args.only.split(',') if s.strip()}
    env = Env(args)
    report = []
    for run in range(args.repeat):
        for number, title, func in SCENARIOS:
            if only and number not in only:
                continue
            before = deadlocks(args)
            started = time.time()
            try:
                result = func(env)
            except Exception as e:  # noqa: BLE001
                result = {'tally': {}, 'ok': False, 'detail': '실행 오류: %s' % e}
            after = deadlocks(args)
            result.update({'scenario': number, 'title': title, 'run': run + 1,
                           'seconds': round(time.time() - started, 1),
                           'deadlocks': None if before is None or after is None else after - before})
            if result['deadlocks']:
                result['ok'] = False
                result['detail'] += ' / 데드락 %d건' % result['deadlocks']
            report.append(result)
            log('[%s] %s  %s  (데드락 %s, %.1fs)\n     응답 %s\n     %s'
                % (number, title, '통과' if result['ok'] else '실패', result['deadlocks'], result['seconds'],
                   result['tally'], result['detail']))
    if args.out:
        with open(args.out, 'w', encoding='utf-8') as f:
            json.dump({'label': args.label, 'base_url': args.base_url, 'report': report}, f,
                      ensure_ascii=False, indent=2)
    failed = [r['scenario'] for r in report if not r['ok']]
    log('\n요약[%s]: %d건 중 실패 %d %s' % (args.label, len(report), len(failed), failed or ''))


if __name__ == '__main__':
    main()
