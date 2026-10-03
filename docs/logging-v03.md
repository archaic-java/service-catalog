# Logging v03: objects and caller-thread trails

Package: `work.archaic.service.logging.v03`. Logging v01 and v02 remain unchanged.
The JDK 25 provider [Culpa](https://github.com/archaic-java/culpa) implements this contract.

## Application API

Implement `Logging` to obtain `logImmediately(String)`, `logOnDebug(String)` and
`logOnFailure(String)` without declaring a logger field. The default `loggingName()` is
`getClass().getName()`; override it to identify an instance. Never infer identity from
`toString()`. Each accepted entry captures its timestamp, source and message at submission.

At composition time, explicitly select one `Log` using `ServiceLoader` and call
`Logging.install(provider)`. The catalog declares `uses Log`; a consumer doing selection
must also declare `uses Log` in its module descriptor. No automatic provider selection or
fallback occurs. Installation is application-wide for this catalog class loader, synchronized
and allowed exactly once; null installation and replacement fail explicitly. Use before
installation throws `IllegalStateException`. Applications requiring isolated logging contexts
may use separately constructed `Log` instances directly, without the global facade.

```java
var providers = ServiceLoader.load(Log.class).stream().toList();
if (providers.size() != 1) throw new IllegalStateException("Exactly one logging provider required");
Logging.install(providers.getFirst().get());
Logging.debug(false);
Logging.trail(() -> reconciler.reconcile(resource));
```

| Call | Meaning |
| --- | --- |
| `logImmediately(message)` | Publish now, with or without a trail. |
| `logOnDebug(message)` | Publish now only if debug is enabled; otherwise discard. |
| `logOnFailure(message)` | Retain evidence in the active trail. |
| `Logging.trail(work)` | Execute synchronously on the calling thread. |
| `Logging.failure(message)` | Mark the active trail failed, without throwing. |
| `Logging.debug(enabled)` | Change the shared provider's debug mode; initially false. |

Debug controls only `logOnDebug`; it does not publish successful trails or duplicate evidence.
The setting is visible across threads. String arguments are eager: their construction happens
even if debug is disabled. All source/message/work arguments must be non-null, including
suppressed debug submissions.

## Execution and failure

A trail establishes a bounded evidence buffer. It creates no thread, submits no task and
uses no executor. It preserves checked exception types through `Work<E extends Exception>`.
Normal completion discards evidence unless explicitly marked failed. An escaping exception
or error publishes one report and is rethrown unchanged. Merely throwing an exception inside
an execution does not mark failure: a caught exception and successful retry are successful.

Nested calls join the active trail. Only the outer boundary publishes and cleans up; an inner
exception caught by the outer work does not independently publish or mark failure. Evidence
from attempted/recovered inner steps remains available if the outer work subsequently fails.
An explicit mark is sticky, keeps the first reason, and publishes at outer completion together
with subsequent evidence. If an exception also escapes, the report includes both the first
explicit reason and the original cause. A failure-valued return or error response does not
implicitly mark failure. Place the boundary around work whose outcome matters, and mark
handled failures explicitly inside it.

`logOnFailure` and `failure` outside an active trail throw `IllegalStateException`.
Independent executions have independent buffers, including concurrent calls sharing objects
and the same provider. Context belongs to an execution, not an object or reusable worker thread.
No supported cross-thread trail propagation exists in v03. Child tasks establish independent
trails. Inherited context must never permit a different thread to mutate the original buffer.
An application owns thread creation and waits for asynchronous work itself.

All sink calls are synchronous on the submitting thread. Providers must not replace an escaping
application exception with a publication failure: attach the sink failure as suppressed where
possible and rethrow the original. If publication of an explicitly marked, normally returning
execution fails, the publication exception escapes. Context must be removed in either case.
Logging does not promise durability or recovery from JVM termination, OOM or process crashes.

## Evidence and retention

`Entry` contains `Instant timestamp`, `String source`, `String message`.
`FailureReport` contains an immutable `List<Entry> evidence` in submission order, the
non-negative count `dropped`, the nullable first `explicitFailure`, and nullable original
`Throwable cause`. At least one failure reason/cause is required. Reports snapshot the list;
throwable identity is preserved, not copied. Timestamp clock adjustments do not reorder entries.

Providers document their finite retention limits, keep the newest evidence and report dropped
entries. Retained source and message fields, including the explicit reason, must also be bounded.
Text clipping must be visible and must not split UTF-16 surrogate pairs. Throwable graphs and
input string construction are outside these bounds. Culpa defaults to 256 evidence entries
and 2048 UTF-16 units per field; clipped text ends with `…`. It retains one separate explicit
reason. Subsequent explicit marks do not accumulate additional reasons.

## Provider conformance

`LoggingV03ProviderContract.cases(collection, fixtureFactory)` in the catalog test module
registers reusable testing-v02 cases. Supply an independent provider, thread-safe observable
entry/report lists, capacity two and field limit 32 for each fixture. Cases check caller-thread
execution, original checked exception/error propagation, successful discard, cleanup, nested
recovery, explicit failure, debug gating, bounded immutable evidence, invalid calls, child thread
isolation and concurrent independent executions. Culpa runs these through Minau alongside
provider-specific checks for service discovery, timestamps, facade binding and sink failures.
