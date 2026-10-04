# Test cases v02

A suite registers independently executable cases through
`void cases(Collection<TestCase> cases)`. A public zero-component record organizes a file;
package-private records in that file implement `void run(TestTrail trail) throws Exception`.
Record components describe inputs and expected results. Each instance is one concrete case.

```java
public record ArithmeticTests() implements TestSuite {
    @Override
    public void cases(Collection<TestCase> cases) {
        cases.add(new Addition(2, 3, 5));
        cases.add(new Addition(-1, 1, 0));
        for (int value = 0; value < 5; value++) {
            cases.add(new Addition(value, 0, value));
        }
    }
}

record Addition(int left, int right, int expected) implements TestCase {
    @Override
    public void run(TestTrail trail) {
        int actual = Math.addExact(left, right);
        trail.note("Actual sum: " + actual);
        assert actual == expected : "Sum must equal " + expected + "; got " + actual;
    }
}
```

Import `java.util.Collection` and the three interfaces from
`work.archaic.service.test.v02`. Only the suite is public. Record types at file scope belong
to the package, so their names must be unique in that package. Records are a convention,
not a requirement enforced by these interfaces.

## Registration and discovery

Discoverable suites are public concrete TestSuite implementations with public
no-argument constructors. Case implementations are invoked through TestCase and may
be package-private; they require no reflective access to their implementation.

For Minau's module selection, JPMS exports/opens, discovery validation and CLI, read
[Minau's discovery reference](https://github.com/archaic-java/minau/blob/main/skills/maintain-minau/references/discovery.md).
Those mechanics belong to the runner. This contract defines the suite/case boundary
and lifecycle independently of a particular runner's scan strategy.

The runner constructs each suite once per run and supplies a fresh mutable collection.
Registration is synchronous: do not retain the collection, modify it from other threads,
or use it after the method returns. The runner takes a validated snapshot before starting
any case from that suite. A constructor, registration or validation failure is a suite
failure; no partially registered cases execute. Empty suites are valid. Null cases are invalid.
Repeated registrations, including identical instances, remain separate executions.

## Execution and evidence

Each registration executes once and may run concurrently with other cases. Create mutable
fixtures and acquire resources inside `run`; record components are only shallowly immutable.
Finish asynchronous work and close resources before returning. There are no suite setup or
teardown hooks in v02. Test resources can use ordinary try-with-resources.

Normal return is success. An escaping exception or error, including AssertionError, is failure.
Run with assertions enabled. Catch and verify expected exceptions inside the case. The runner
preserves the original failure and includes its trail in the failure report. Successful trails
are discarded. A case's `toString()` supplies its description; runners distinguish registrations
independently of descriptions so duplicate examples are not collapsed. Keep descriptions pure
and useful; default record descriptions expose component values, so avoid secrets in case data.

`TestTrail.note(String)` records observations in order. It accepts only non-null text and is
valid only on the case's executing thread during `run`. Cross-thread and completed-trail use
throw IllegalStateException. The runner documents storage bounds and reports omitted or
truncated observations. The test trail is independent of logging Diagnostics, Goal and Trail;
application goals can execute within a case without nesting into a test logging scope.

## Compatibility

The v01 package remains unchanged. Runners may support both versions, but a suite should
implement only one version. Moving to v02 replaces annotated methods with explicit registered
objects. A runner must not require a dependency on a particular logging provider.
