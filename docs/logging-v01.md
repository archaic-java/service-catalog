# Goal-scoped diagnostics v01

`work.archaic.service.logging.v01` defines the first contract for collecting diagnostic
evidence during an operation and retaining it only when the operation fails. It has no
SLF4J dependency, severity levels, MDC or fluent logging builder.

## Contract boundary

| Type | Responsibility |
| --- | --- |
| `Goal` | Reusable named objective; `run`, `call`, and a virtual-thread executor adapter |
| `Trail` | Bounded, thread-confined observations for one active execution |
| `Log` | Destination for immediate information and completed failure reports |
| `GoalProvider` | Creates goals and exposes its current trail to participating code |
| `Observation` | Captured text and monotonic elapsed time |
| `FailureReport` | Execution identity, timing, retained evidence, loss counts and original failure |

Goal is an interface in the catalog. Whether a concrete provider offers `new Goal(...)`, a
factory, or another convenience facade remains a provider API choice. The catalog implements
only data validation and trivial delegation; scope binding, buffers, scheduling and output
belong to a separate library. A provider can use Java 25 ScopedValue internally without
exposing its binding key. No reflection, internal JDK API or preview feature is needed.

Log provides two output paths: `note(String)` publishes general information immediately,
and `write(FailureReport)` accepts the evidence from a failed execution. Neither uses levels.
Unlike `Trail.note`, `Log.note` works outside a goal and remains immediate inside one; a
successful goal never discards it. Both methods report destination I/O failures explicitly.
Immediate publication does not guarantee durable storage. Audit-specific guarantees are
outside v01.

```java
log.note("Application started; listening on port 8080");
```

## Lifecycle

Creating a goal starts no execution. Each run/call or executor task gets fresh state and a
distinct execution UUID. Synchronous calls use the calling thread; executor tasks each use
a fresh virtual thread. Reusing the same goal concurrently is supported.

Normal completion discards diagnostic evidence. An escaping Throwable, including a checked
exception or Error, causes one attempt to write a failure report before the original throwable
is propagated unchanged. Report publication is excluded from execution duration. If writing
fails, preserve the work failure and add the reporting failure as suppressed when possible;
do not retry automatically. This is best effort, not a guarantee during VM/resource failure.
Recovering from an exception and returning normally is success in this version.

The execution boundary completes the invocation and releases its scope on all exit paths.
There is no public finish/close/fail method on a goal or trail. Retained trail references reject
use after completion or from another thread. Access outside a goal fails explicitly rather
than silently dropping observations. Nested synchronous goals are rejected before their work
runs in v01. Independently dispatched tasks receive fresh roots; no parent relationship,
inheritance or propagation is defined. Child goals can be designed in a later contract version.

Providers document maximum retained entry count, message size and eviction policy. Retained
observations stay in capture order; dropped and truncated evidence is counted. Buffering is
eager: successful operations still pay capture costs. Report lists are immutable copies;
Throwable retains its original identity and is not deeply immutable.

## Selection and use

Applications may explicitly construct a provider or load `GoalProvider` via ServiceLoader.
For service loading, the consumer declares `uses work.archaic.service.logging.v01.GoalProvider`;
the provider declares `provides ...GoalProvider with ...Implementation`. A Log may also be
selected with its own uses/provides pair, or supplied directly as an implementation. Log has
two abstract methods and is not a functional interface.
Selection belongs to the application: require exactly one provider unless explicitly selecting
by provider type; zero or ambiguous providers are configuration errors, never an arbitrary
first match. The catalog itself performs no discovery and adds no global state.

Given an explicitly selected provider and log:

```java
Goal goal = provider.goal("orders.create", log);
Order order = goal.call(() -> {
    provider.note("Checking inventory");
    return orders.create();
});
```

`Goal.run` and `Goal.call` preserve checked failure types without wrapping. Application code can
retain the shared provider for observations; it must not retain a trail across executions. A
future provider facade may offer static `Trail.note(...)` convenience. That earlier sketch is
not a static method on this catalog's Trail interface; no provider implementation is silently
selected by calling a catalog method.

## JDK HTTP server

```java
var goal = provider.goal("http.request", log);
var executor = goal.executor();
server.setExecutor(executor);
server.createContext("/orders", exchange -> {
    try (exchange) {
        provider.note("Loading orders");
        byte[] body = loadOrders();
        exchange.sendResponseHeaders(200, body.length);
        try (var output = exchange.getResponseBody()) {
            output.write(body);
        }
    }
});
server.start();
// During application shutdown: server.stop(delay), then executor.close().
```

The executor owns completion of each submitted task, not knowledge of HTTP outcomes. Exceptions
caught inside the server task and HTTP 500 statuses do not become escaping failures. A future
HTTP adapter must observe those at the handler/filter boundary. This catalog makes no promise
of detecting them, of exactly one task per exchange for every server provider, or of remote
receipt of a response. Synchronous handlers must finish response writing/closing before
returning for task completion to cover that work. Do not wrap the handler in a second goal
when the executor has already established one.

For generic executors, submit/invoke tasks must be instrumented around the actual user work,
inside Future exception capture. Merely wrapping FutureTask.run or installing an uncaught
exception handler is insufficient. Cancelled-before-start and rejected tasks produce no
execution report; cancellation of started work is cooperative and reporting follows its
actual completion. An interrupted operation that returns normally is considered successful.

## Future capabilities

JFR measurements and tracing should observe each execution, including successful ones. Trail
retention must not bias those measurements. Execution IDs and stable names provide an initial
correlation basis, but this PR does not define metric instruments, tracing propagation,
parent/child goals, arbitrary attributes or plugin lifecycle callbacks. Published v01 contracts
are immutable; new incompatible capabilities require a new version.

## Validation and provider follow-up

With JDK 25:

```sh
javac @cmd/compile
java @cmd/test
```

The separate test module validates report copying/invariants, default action delegation,
checked exception identity and Error propagation. It also compiles JDK HTTP integration against
the exported contract, including immediate Log.note calls with checked IOException handling.
These tests do not claim to validate a provider that does not exist yet.

The first implementation must add contract-level tests for:

- success discard, complete failure reports and original exception identity;
- reporting failure without replacing the work failure;
- immediate information inside and outside goals, independent of trail retention, with explicit
  output failure propagation and coherent concurrent messages/reports;
- independent concurrent/repeated executions and unique execution IDs;
- scope cleanup on every path, stale/wrong-thread trail rejection and unbound access;
- buffer limits, truncation and omitted evidence counts;
- virtual-thread execution, execute/submit failure handling, cancellation and shutdown;
- nesting rejection without invoking the nested callback.

An HTTP integration test must explicitly demonstrate the difference between an escaping task
failure and one handled internally by the server. Once this PR is accepted, implement these
contracts and the scope/output mechanics in the separate provider library.
