"""
Standalone, reproducible proof-of-concept for the indexing benchmark
documented in docs/design-decisions.md. Requires nothing beyond the Python
standard library (sqlite3). Not part of the Spring Boot application —
IndexingBenchmarkTest in src/test reproduces the equivalent comparison
against this project's own H2 schema.

Run: python3 sqlite_indexing_proof.py
"""
import sqlite3
import random
import time
import statistics

random.seed(42)
N = 100_000


def build_db(with_index):
    conn = sqlite3.connect(":memory:")
    c = conn.cursor()
    c.execute("DROP TABLE IF EXISTS orders")
    c.execute("""CREATE TABLE orders (
        id INTEGER PRIMARY KEY,
        customer_id INTEGER,
        status TEXT,
        total REAL,
        created_at TEXT
    )""")
    rows = []
    statuses = ["PLACED", "SHIPPED", "DELIVERED", "CANCELLED", "RETURNED"]
    for i in range(N):
        rows.append((i, random.randint(1, 20000), random.choice(statuses),
                     round(random.uniform(200, 9000), 2), "2026-01-01"))
    c.executemany("INSERT INTO orders VALUES (?,?,?,?,?)", rows)
    if with_index:
        c.execute("CREATE INDEX idx_customer_status ON orders(customer_id, status)")
    conn.commit()
    return conn


def bench(conn, lookups):
    c = conn.cursor()
    times = []
    for cid, status in lookups:
        t0 = time.perf_counter()
        c.execute("SELECT * FROM orders WHERE customer_id=? AND status=?", (cid, status))
        c.fetchall()
        t1 = time.perf_counter()
        times.append((t1 - t0) * 1000)
    return times


def percentile(times, pct):
    s = sorted(times)
    k = int(len(s) * pct / 100)
    return s[min(k, len(s) - 1)]


def main():
    conn_noidx = build_db(with_index=False)
    conn_idx = build_db(with_index=True)

    c = conn_idx.cursor()
    c.execute("SELECT customer_id, status FROM orders ORDER BY RANDOM() LIMIT 200")
    lookups = c.fetchall()

    # warm up
    bench(conn_noidx, lookups[:20])
    bench(conn_idx, lookups[:20])

    times_noidx = bench(conn_noidx, lookups)
    times_idx = bench(conn_idx, lookups)

    print(f"Rows: {N}")
    print(f"WITHOUT index: p50={statistics.median(times_noidx):.3f}ms  "
          f"p95={percentile(times_noidx, 95):.3f}ms  mean={statistics.mean(times_noidx):.3f}ms")
    print(f"WITH index:    p50={statistics.median(times_idx):.3f}ms  "
          f"p95={percentile(times_idx, 95):.3f}ms  mean={statistics.mean(times_idx):.3f}ms")
    print(f"Speedup (mean): {statistics.mean(times_noidx) / statistics.mean(times_idx):.1f}x")

    c2 = conn_noidx.cursor()
    c2.execute("EXPLAIN QUERY PLAN SELECT * FROM orders WHERE customer_id=? AND status=?", (1, "PLACED"))
    print("\nWithout index plan:", c2.fetchall())
    c3 = conn_idx.cursor()
    c3.execute("EXPLAIN QUERY PLAN SELECT * FROM orders WHERE customer_id=? AND status=?", (1, "PLACED"))
    print("With index plan:", c3.fetchall())


if __name__ == "__main__":
    main()
