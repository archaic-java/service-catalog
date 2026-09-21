# Goal-scoped diagnostics v02

An application can fulfill many goals. A Goal describes one such intent, such as placing an
order. Each execution is an independent attempt to fulfill it, including communicating the
outcome when that belongs to the intent. Construction starts no work. The same goal can be
used repeatedly and concurrently.

## Vocabulary

| Type | Responsibility |
| --- | --- |
| `Diagnostics` | Defines goals and exposes the current attempt's trail |
| `Goal` | Reusable named intent, executed with `run(Action)` or `newExecutor()` |
| `Trail` | Bounded, thread-confined notes for one active attempt |
| `Observation` | Captured note and monotonic elapsed time |
| `Log` | Writes immediate messages and completed failure reports |
| `FailureReport` | Execution identity, goalName, timing, retained observations, loss counts and original failure |

`Diagnostics.note(String)` delegates to `currentTrail().note(String)`. Both collect temporary
evidence. `Log.write(String)` publishes immediate information independently of the current goal;
`Log.write(FailureReport)` publishes completed failure evidence. There are no severity levels.
Both Log overloads report output failures through IOException; immediate publication does not
guarantee durable storage. Log implementations must support concurrent calls and coherent output.

## Execution and failure

```java
Diagnostics diagnostics = exactlyOne(Diagnostics.class);
Log log = exactlyOne(Log.class);
Goal placeOrder = diagnostics.goal("orders.place", log);

log.write("Application started; listening on port 8080");
placeOrder.run(() -> {
    diagnostics.note("Checking inventory");
    Order order = orders.create(request);
    respondWithConfirmation(order);
});
```

`run` executes on the calling thread and preserves declared checked exception types. It has no
result. Ordinary methods inside the goal may return values; keep handling of the complete intent
inside its boundary.

Normal completion means success and discards the trail. An escaping exception or error means
failure: offer one report to the Log, then rethrow the original throwable unchanged. Reporting
failures are suppressed on the original when possible, never substituted for it or retried.
There is no explicit outcome, finish, fail or close method on a goal.

When a failed attempt needs a response, communicate it and rethrow inside the goal:

```java
placeOrder.run(() -> {
    try {
        Order order = orders.create(request);
        respondWithConfirmation(order);
    } catch (OrderStorageException failure) {
        try {
            respondWithFailure(failure);
        } catch (IOException responseFailure) {
            failure.addSuppressed(responseFailure);
        }
        throw failure;
    }
});
```

These examples use application-defined order and response methods. Catching a failure and
returning normally counts as success. HTTP status codes and failure values do not implicitly
change the outcome. Response failures escaping the boundary also fail the goal.

Each attempt owns a UUID, timing and fresh trail. Trails reject use outside their execution or
on another thread. Providers document entry and message limits and eviction policy; omitted and
truncated observations are counted in the report. Capture is eager even for successful attempts.
Reports defensively copy observations but retain the original Throwable by identity, read-only
for recipients. No nesting, inheritance or implicit cross-thread propagation is supported.

## Executors and service loading

`Goal.newExecutor()` creates a caller-owned ExecutorService using a fresh virtual thread and
independent goal execution per started task. Close waits for termination and does not close the
goal, other executors or Log. The standard ExecutorService methods, including Callable result
methods, remain available for JDK interoperability; Goal itself has no result-returning method.
Failures must be observed inside Future exception capture. Rejected and cancelled-before-start
tasks have no execution/report; an escaping interruption from running work is failure. Exceptions
caught by a task, including FutureTask or an HTTP server's internal task, cannot be observed.

For HTTP handler diagnostics, use `goal.run(() -> handler.handle(exchange))` at the handler
boundary with an ordinary server executor. Keep response handling inside the goal. Do not also
wrap the server executor in a goal, since nested goals are rejected.

Consumers require `work.archaic.service.catalog` and declare:

```java
uses work.archaic.service.logging.v02.Diagnostics;
uses work.archaic.service.logging.v02.Log;
```

Select providers explicitly with ServiceLoader, requiring exactly one unless choosing by type.
Share the selected Diagnostics with participating code. There is no global registry or implicit
first-provider selection. Implementations, scope bindings, buffers and scheduling belong to
provider libraries. Consumer configuration belongs to the catalog contract.

## Compatibility and checks

v01 is unchanged. Migration replaces GoalProvider with Diagnostics, Log.note with Log.write,
Goal.executor with Goal.newExecutor, and FailureReport.goal with FailureReport.goalName. Goal.call
and Goal.Operation are removed; Goal.Action and run remain. Trail.note is unchanged.

Compile with `javac @cmd/compile`, then run `java @cmd/test` with JDK 25. Catalog checks cover both
versions' report invariants and default delegation. Providers must additionally verify lifecycle,
exception identity, failure response/rethrow, concurrent executions, scope cleanup, evidence
bounds, executor methods/cancellation and immediate output inside and outside goals. Published
versioned contracts are immutable; incompatible changes require a subsequent version.
