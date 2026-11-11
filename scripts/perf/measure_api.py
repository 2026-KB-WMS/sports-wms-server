#!/usr/bin/env python3
"""API별 쿼리 횟수와 서버 처리시간을 같은 조건으로 재서 표로 보여 준다. 수정 전후 개선율 비교용.

서버가 docker-compose로 떠 있고(컨테이너 wms-app) 쿼리 횟수를 찍는 ApiTimingFilter 로그를 읽는다.
쿼리 횟수는 항상 같은 값이라 개선율의 주 지표이고, 시간은 로컬 환경 영향이 있어 중앙값과 p95를 함께 본다.

사용법
    python scripts/perf/measure_api.py --label before --out before.json
    (코드를 고치고 컨테이너를 다시 빌드한 뒤)
    python scripts/perf/measure_api.py --label after --out after.json --compare before.json

측정 대상은 TARGETS에 있다. 각 대상을 워밍업 후 --repeat번 호출하고, 같은 시간 구간의 로그에서
"GET /api/v1/... -> 200 (123ms, queries=N)" 줄을 읽는다. 다른 요청이 섞이지 않도록 서버를 쓰지 않을 때 실행한다.
"""
import argparse
import json
import os
import re
import statistics
import subprocess
import sys
import urllib.request
from datetime import datetime, timezone

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..'))
import seed_mock_data as base  # noqa: E402

# (이름, 호출 경로, 로그에 찍히는 경로 패턴)
TARGETS = [
    ('상품 목록 size=20', '/products?size=20', '/api/v1/products'),
    ('상품 목록 size=100', '/products?size=100', '/api/v1/products'),
    ('SKU 목록 size=20', '/products/skus?size=20', '/api/v1/products/skus'),
    ('SKU 목록 size=100', '/products/skus?size=100', '/api/v1/products/skus'),
    ('창고 구역 목록(전체)', '/warehouses/sections', '/api/v1/warehouses/sections'),
    ('창고 구역 목록(창고별)', '/warehouses/{warehouseId}/sections', '/api/v1/warehouses/{warehouseId}/sections'),
    ('지점 담당자 목록 size=20', '/stores/managers?size=20', '/api/v1/stores/managers'),
    ('지점 담당자 목록 size=100', '/stores/managers?size=100', '/api/v1/stores/managers'),
    ('창고 담당자 목록 size=20', '/warehouses/managers?size=20', '/api/v1/warehouses/managers'),
    ('창고 담당자 목록 size=100', '/warehouses/managers?size=100', '/api/v1/warehouses/managers'),
]
LOG_LINE = re.compile(r'(GET) (\S+) -> (\d+) \((\d+)ms, queries=(\d+)\)')
WARMUP = 5


def call(base_url, path, token):
    req = urllib.request.Request(base_url + '/api/v1' + path, headers={'Authorization': 'Bearer ' + token})
    with urllib.request.urlopen(req) as resp:
        resp.read()


def server_lines(since, pattern, container):
    out = subprocess.run(['docker', 'logs', '--since', since, container], capture_output=True, text=True,
                         encoding='utf-8')
    rows = []
    for line in (out.stdout + out.stderr).splitlines():
        m = LOG_LINE.search(line)
        if m and m.group(2) == pattern and m.group(3) == '200':
            rows.append((int(m.group(4)), int(m.group(5))))
    return rows


def percentile(values, pct):
    ordered = sorted(values)
    return ordered[min(len(ordered) - 1, int(round(pct / 100 * (len(ordered) - 1))))]


def measure(args, token):
    results = {}
    # 경로의 {warehouseId}는 첫 번째 창고로 채운다. 로그 패턴은 그대로 둔다.
    warehouse_id = base.Api(args.base_url).get('/warehouses', token)['items'][0]['warehouseId']
    for name, path, pattern in TARGETS:
        path = path.replace('{warehouseId}', str(warehouse_id))
        for _ in range(WARMUP):
            call(args.base_url, path, token)
        since = datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%S.%fZ')
        for _ in range(args.repeat):
            call(args.base_url, path, token)
        rows = server_lines(since, pattern, args.container)[-args.repeat:]
        if len(rows) < args.repeat:
            sys.exit('%s: 서버 로그에서 %d건만 찾았습니다(%d건 필요). ApiTimingFilter 로그가 켜져 있는지 확인하세요.'
                     % (name, len(rows), args.repeat))
        times, queries = [r[0] for r in rows], {r[1] for r in rows}
        results[name] = {'queries': sorted(queries)[-1], 'queriesVary': len(queries) > 1,
                         'medianMs': statistics.median(times), 'p95Ms': percentile(times, 95)}
    return results


def pct_change(before, after):
    return '-' if not before else '%+.0f%%' % ((after - before) / before * 100)


def print_table(label, results, compare):
    print('\n[%s] 반복 횟수와 환경은 같은 조건이어야 비교할 수 있습니다.' % label)
    if compare:
        print('| API | 쿼리 | 쿼리 변화 | 중앙값(ms) | 중앙값 변화 | p95(ms) | p95 변화 |')
        print('|---|---|---|---|---|---|---|')
    else:
        print('| API | 쿼리 | 중앙값(ms) | p95(ms) |')
        print('|---|---|---|---|')
    for name, r in results.items():
        mark = '*' if r['queriesVary'] else ''
        if compare and name in compare:
            b = compare[name]
            print('| %s | %d → %d%s | %s | %g → %g | %s | %g → %g | %s |' % (
                name, b['queries'], r['queries'], mark, pct_change(b['queries'], r['queries']),
                b['medianMs'], r['medianMs'], pct_change(b['medianMs'], r['medianMs']),
                b['p95Ms'], r['p95Ms'], pct_change(b['p95Ms'], r['p95Ms'])))
        else:
            print('| %s | %d%s | %g | %g |' % (name, r['queries'], mark, r['medianMs'], r['p95Ms']))
    if any(r['queriesVary'] for r in results.values()):
        print('* 호출마다 쿼리 수가 달랐습니다(가장 큰 값을 표시).')


def main():
    parser = argparse.ArgumentParser(description='API별 쿼리 횟수·서버 처리시간 측정')
    parser.add_argument('--base-url', default='http://localhost:8080')
    parser.add_argument('--admin-id', default=base.read_env('WMS_ADMIN_LOGIN_ID') or 'admin')
    parser.add_argument('--admin-password', default=base.read_env('WMS_ADMIN_PASSWORD'))
    parser.add_argument('--container', default='wms-app')
    parser.add_argument('--repeat', type=int, default=30, help='대상마다 측정 호출 횟수(워밍업 5회 별도)')
    parser.add_argument('--label', default='measure')
    parser.add_argument('--out', help='결과를 저장할 JSON 파일')
    parser.add_argument('--compare', help='비교할 이전 결과 JSON 파일')
    args = parser.parse_args()
    if not args.admin_password:
        sys.exit('본사 관리자 비밀번호가 필요합니다(.env의 WMS_ADMIN_PASSWORD 또는 --admin-password).')

    token = base.Api(args.base_url).login(args.admin_id, args.admin_password)
    results = measure(args, token)
    compare = json.load(open(args.compare, encoding='utf-8')) if args.compare else None
    print_table(args.label, results, compare)
    if args.out:
        json.dump(results, open(args.out, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
        print('\n결과 저장: %s' % args.out)


if __name__ == '__main__':
    main()
