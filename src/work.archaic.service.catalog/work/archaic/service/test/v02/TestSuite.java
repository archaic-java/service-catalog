package work.archaic.service.test.v02;

import java.util.Collection;

/**
 * Registers the independently executable cases belonging to a suite.
 *
 * <p>Discoverable suites are public concrete types with a public no-argument constructor.
 * A zero-component record is a convenient implementation. The runner constructs one instance
 * per suite and calls {@link #cases(Collection)} once per run.
 */
public interface TestSuite {
    /**
     * Adds cases to a fresh, mutable, runner-owned collection.
     *
     * <p>Registration is synchronous. Do not retain or use the collection after this method
     * returns, or mutate it from another thread. The runner snapshots its contents before
     * executing any case from this suite. Null cases are rejected; duplicates are independent
     * registrations and must not be deduplicated. An empty suite is valid.
     *
     * <p>If construction, registration or snapshot validation fails, no case from this suite
     * runs and the runner reports a suite failure. Acquire case resources inside
     * {@link TestCase#run(TestTrail)}, not during registration.
     *
     * @param cases initially empty collection accepting test cases
     */
    void cases(Collection<TestCase> cases);
}
