# N+1 optimisation benchmark: recorded runs

`NPlusOneBenchmarkTest` measures the effect of the `LEFT JOIN FETCH` queries
described in Section 6.5 of the project report. It builds 30 master resumes of
6 sections each, runs 5 warm-up iterations, then reports the median of 20
measured iterations using Hibernate's own statistics.

Reproduce with:

```bash
mvn -pl resume-service test -Dtest=NPlusOneBenchmarkTest
```

## Why this file exists

The statement count and the latency behave differently under repetition, and
the report quotes both, so it is worth being explicit about which one is a
measurement and which one is an observation.

The **statement count is deterministic**. Lazy loading issues 31 queries (one
for the parent rows, one per resume) and the fetch join collapses them to 1, on
every run, on every machine. That is a property of the mapping and the query,
not of the hardware.

The **latency is not reproducible**. It depends on JIT warm-up, heap state,
what else the machine is doing, and the in-memory database's own caching. Three
runs on the same machine on the same commit produced:

| Run | Lazy (31 queries) | Fetch join (1 query) | Improvement |
|-----|-------------------|----------------------|-------------|
| 1   | 3.487 ms          | 1.126 ms             | 67.7% |
| 2   | 2.427 ms          | 0.858 ms             | 64.6% |
| 3   | 4.647 ms          | 1.073 ms             | 76.9% |

The report quotes run 1. A reader re-running the test should expect figures in
this range rather than those exact numbers, which is why Section 6.5 states
that the statement count is the stable measure and the timings are not
reproducible.

## A note on what the benchmark does not show

The measurement runs against an in-memory database in the same process, so an
eliminated statement costs almost nothing beyond the call itself. Against a
networked database each eliminated statement would also remove a round trip,
so the proportional gain reported here understates the effect in a deployed
system rather than overstating it.
