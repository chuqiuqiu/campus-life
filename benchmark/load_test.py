#!/usr/bin/env python3
"""Simple closed-loop HTTP load generator used for the README benchmark.

It keeps `concurrency` requests in flight, records every latency sample and
writes a machine-readable JSON summary. No external load-testing binary needed.
"""
import argparse
import asyncio
import json
import time
from collections import Counter


def percentile(sorted_values, p):
    if not sorted_values:
        return 0.0
    k = (len(sorted_values) - 1) * p
    lo = int(k)
    hi = min(lo + 1, len(sorted_values) - 1)
    return sorted_values[lo] + (sorted_values[hi] - sorted_values[lo]) * (k - lo)


async def _worker(session, url, end_time, latencies, statuses):
    while time.perf_counter() < end_time:
        started = time.perf_counter()
        try:
            async with session.get(url) as response:
                await response.read()
                statuses[response.status] += 1
        except Exception as exc:  # timeouts / connection resets
            statuses[type(exc).__name__] += 1
        finally:
            latencies.append((time.perf_counter() - started) * 1000.0)


async def run_level(url, concurrency, duration, warmup, timeout):
    connector = aiohttp.TCPConnector(limit=concurrency, ttl_dns_cache=300)
    client_timeout = aiohttp.ClientTimeout(total=timeout, connect=5)
    async with aiohttp.ClientSession(connector=connector, timeout=client_timeout) as session:
        # Warm-up: populate caches / JIT / connection pool and discard samples.
        if warmup > 0:
            warm_end = time.perf_counter() + warmup
            await asyncio.gather(*[
                _worker(session, url, warm_end, [], Counter()) for _ in range(concurrency)
            ])

        latencies = []
        statuses = Counter()
        started = time.perf_counter()
        end_time = started + duration
        await asyncio.gather(*[
            _worker(session, url, end_time, latencies, statuses) for _ in range(concurrency)
        ])
        elapsed = time.perf_counter() - started

    latencies.sort()
    count = len(latencies)
    ok = statuses.get(200, 0)
    errors = count - ok
    return {
        'concurrency': concurrency,
        'duration_s': round(elapsed, 3),
        'requests': count,
        'qps': round(count / elapsed, 2) if elapsed else 0,
        'avg_ms': round(sum(latencies) / count, 2) if count else 0,
        'p50_ms': round(percentile(latencies, 0.50), 2),
        'p90_ms': round(percentile(latencies, 0.90), 2),
        'p95_ms': round(percentile(latencies, 0.95), 2),
        'p99_ms': round(percentile(latencies, 0.99), 2),
        'max_ms': round(latencies[-1], 2) if latencies else 0,
        'error_count': errors,
        'error_rate_pct': round(errors * 100.0 / count, 2) if count else 0,
        'statuses': dict(statuses),
    }


async def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--url', required=True)
    parser.add_argument('--levels', default='1,10,50,100,200')
    parser.add_argument('--duration', type=float, default=20)
    parser.add_argument('--warmup', type=float, default=5)
    parser.add_argument('--timeout', type=float, default=10)
    parser.add_argument('--repeat', type=int, default=1)
    parser.add_argument('--output', required=True)
    args = parser.parse_args()

    levels = [int(x) for x in args.levels.split(',') if x.strip()]
    results = []
    all_runs = []
    for concurrency in levels:
        samples = []
        for run in range(args.repeat):
            row = await run_level(args.url, concurrency, args.duration, args.warmup, args.timeout)
            row['run'] = run + 1
            samples.append(row)
            print(json.dumps(row, ensure_ascii=False), flush=True)
        # Keep every run for auditability, and use the median QPS run as summary.
        all_runs.append(samples)
        samples.sort(key=lambda r: r['qps'])
        results.append(samples[len(samples) // 2])

    with open(args.output, 'w', encoding='utf-8') as fh:
        json.dump({'url': args.url, 'levels': results, 'runs': all_runs}, fh, ensure_ascii=False, indent=2)
    print('written', args.output)


if __name__ == '__main__':
    import aiohttp
    asyncio.run(main())