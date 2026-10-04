# Logging v03: configured logging contexts

Package: `work.archaic.service.logging.v03`, JDK 25, no preview features.
[Culpa](https://github.com/archaic-java/culpa) implements the provider contract.
Logging v01 and v02 remain unchanged. There is no global installation, global debug state,
or separate trail boundary in v03.

## Contents

- [Select a provider and create a context](#select-a-provider-and-create-a-context)
- [Log from application objects](#log-from-application-objects)
- [Scope and outcome](#scope-and-outcome)
- [Evidence and retention](#evidence-and-retention)
- [Implement and verify a provider](#implement-and-verify-a-provider)


## Select a provider and create a context

`Log` is a stateless factory for independent `Context` instances. Consumers require
`work.archaic.service.catalog`, declare `uses work.archaic.service.logging.v03.Log`
and select a provider explicitly through ServiceLoader. Resolve its provider module at launch.

```java
var providers = ServiceLoader.load(Log.class).stream().toList();
if (providers.size() != 1) throw new IllegalStateException("Exactly one logging provider required");
var logging = providers.getFirst().get();
var context = logging.context();
context.run(() -> reconciler.reconcile(resource));
// Use a fresh context for work that returns a value:
var result = logging.context().call(() -> reconciler.read(resource));
```

`logging.context()` captures the provider's documented defaults. To select per-context output,
debug, clock and retention settings, use `logging.context(configuration)`:

```java
var configuration = Configuration.text(true, System.err);
var context = logging.context(configuration);
```

`Configuration.text(debug, stream)` uses the reusable catalog `TextOutput` renderer.
It writes timestamp, source and message, escaping backslashes, CR and LF in fields.
Reports include retained evidence, dropped count, explicit reason and original stack trace.
Each complete report and entry synchronizes on the selected stream, even across separate
renderer instances. The caller owns the stream; rendering never closes it and follows
PrintStream error semantics. For custom clocks/limits, construct `TextOutput` explicitly and
pass `output::entry` and `output::failure` to the full Configuration constructor.
Custom rendering remains supported by supplying application-defined sinks.

The three-argument constructor chooses UTC, 256 retained entries and 2048 UTF-16 units per
field. The full constructor is `(boolean debug, Clock clock, Consumer<Entry> entries,
Consumer<FailureReport> failures, int capacity, int fieldLimit)`. All references are required;
capacity must be >= 1 and field limit >= 2. The record is immutable, but clocks and sink targets
may contain mutable state. Shared sinks must support concurrent contexts. Configuration can
be reused; each context is single-use. No provider-global mutable configuration is needed.

## Log from application objects

Implement `Logging` to obtain defaults without logger fields. `loggingName()` defaults to
`getClass().getName()`; override it for instance names, rather than deriving identity from
the object's toString(). Default methods use the currently bound logging context.

| Call | Meaning |
| --- | --- |
| `logImmediately(String)` | Publish now within the active context. |
| `logOnDebug(Supplier<String>)` | Compute and publish now only if this context enables debug. |
| `logOnFailure(String)` | Retain evidence for publication if this context fails. |
| `Logging.context()` | Access the current context. |
| `context.fail(String)` | Mark failed without throwing; keep the first reason. |
| `context.run(Work<E>)` | Run void work once synchronously on the calling thread. |
| `context.call(Call<T, E>)` | Run value-returning work with the same lifecycle. |

For example: `logOnDebug(() -> expensiveStateDescription())`.
A non-null supplier is required even with debug disabled, but its body is never executed in
that case. With debug enabled, evaluate it exactly once, on the calling thread. Reject a null
result; propagate supplier exceptions/errors without wrapping. If they escape `run`, the context
fails just like any other escaping failure. Lambda creation and expressions outside its body
are still eager; put expensive work inside the body. Debug controls only debug output and does
not publish successful contexts. No String overload exists for logOnDebug.

All message/source arguments must be non-null. Capture timestamps and names at submission;
for lazy debug, capture the entry after the supplier produces its message. Retain evidence in
submission order even if the clock moves backwards.

## Scope and outcome

Creation performs no execution or output. `Context.run` binds the context on the calling
thread; it creates no thread, executor or asynchronous task. `Work<E extends Exception>`
and `Call<T, E extends Exception>` preserve checked exception types. `call` returns the exact
value, including null, after completion and output publication. It does not inspect result values
for success or failure; use `fail` for a handled failure. `run` delegates to the same lifecycle.
Calling either method consumes the context; reuse through either method is rejected. A context can be created on one thread and run on another,
but after execution starts, every logging operation belongs to the executing thread.

Normal completion discards evidence unless marked failed. An exception or error escaping the
scope publishes one failure report and is rethrown unchanged. Merely throwing inside the work
does not mark failure: a caught exception and successful retry can complete successfully.
`context.fail(reason)` is sticky, keeps the first reason, and publishes at completion including
subsequent evidence. If an exception also escapes, report both the first reason and original
cause. Error responses and failure-valued returns do not implicitly mark failure.

Each context runs exactly once. Reentrant, concurrent or completed-context reuse throws
IllegalStateException. Null work for either run or call is rejected before the context is consumed. Context operations
before run, after completion, on another thread, or through a suspended outer context fail
explicitly. Application logging outside an active scope also fails; there is no implicit fallback.

Nested contexts are independent. The inner context temporarily replaces the current binding
and owns its evidence, configuration and outcome. Scope exit restores the outer binding before
publication and before propagating a failure. If the outer operation catches and recovers from
the inner failure, only the inner context fails. If the same exception escapes the outer scope,
both contexts fail and publish their own evidence with that original cause. Reuse the current
context across ordinary method calls without introducing another context for every helper.

Ordinary child threads do not inherit logging context. A child task establishes its own scope.
A parent becomes failed when an observed child failure escapes the parent scope, or when it
is explicitly marked. Contexts remain thread-confined; cross-thread sharing and structured
concurrency are outside this version. Logging creates no concurrency requirement.

All sinks run synchronously. If publication fails while an application exception is escaping,
attach the output failure as suppressed where possible and preserve the original throwable.
If output fails after a marked, normally returning execution, that output error escapes.
Bindings and retained evidence must be cleaned up in both cases. Output callbacks must not
reenter the completing context. Logging implies no transactions, rollback, retries or durability;
JVM termination, OOM and process crashes may prevent publication.

## Evidence and retention

`Entry` contains Instant timestamp, String source and String message. `FailureReport` holds
an immutable snapshot List<Entry> evidence, a non-negative dropped count, nullable first
explicitFailure Entry and nullable original Throwable cause. At least one reason/cause is
required. Throwable identity is preserved; its mutable graph is not copied.

Keep the newest evidence entries up to configuration.capacity and report the exact eviction
count. Bound retained source/message fields, including the explicit reason, to fieldLimit;
mark clipping visibly without splitting UTF-16 surrogate pairs. Keep only one explicit reason.
The bounds do not cover caller-created input strings, throwable graphs or sink-owned snapshots.
Release retained evidence after completion, including successful completion and output errors.

## Implement and verify a provider

Implement Log to create contexts. Extend Context, implementing immediately, onFailure, fail
and finish. The final run/call methods supply single-use lifecycle, scoped binding, restoration and
original-throwable preservation; the final onDebug method supplies lazy evaluation. Call the
protected requireActive guard before each provider logging operation. finish(cause) runs once
outside the completed binding; publish if marked failed or cause is non-null, and release evidence
in a finally block. Providers must honor the supplied configuration and document default output.

`LoggingV03ProviderContract.cases(collection, fixtureFactory)` in the catalog test module
registers testing-v02 conformance cases. Supply an independent provider, clock and thread-safe
observed entry/report lists per fixture. Tests configure their own contexts and exercise caller-
thread execution, single-use lifecycle, explicit/escaping failure, nested recovery, independent
concurrent scopes, child-thread confinement, bounded immutable evidence, object attribution and
lazy debug (disabled, exactly-once enabled, null result and failing suppliers), plus nullable
return values, checked call exceptions, mixed run/call reuse and nested returning work.
The standalone catalog checks also verify standard text output and serialization across renderers. Culpa runs these
through Minau alongside discovery, timestamp, custom-configuration and sink-failure checks.
