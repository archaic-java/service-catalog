package work.archaic.service.test.v02;

/**
 * One independently executable verification of behavior.
 *
 * <p>Records naturally describe case inputs and expected results. Case implementations may be
 * package-private: runners invoke this interface directly, without reflective access to their
 * implementation. The case's {@code toString()} supplies its display description; runners must
 * distinguish duplicate registrations independently of that description.
 *
 * <p>Cases may execute concurrently. Keep inputs immutable and create mutable fixtures within
 * {@link #run(TestTrail)}. Record components are only shallowly immutable. Each registration is
 * executed once per run, even when the same instance is registered more than once.
 */
public interface TestCase {
    /**
     * Verifies the case with a fresh trail. Normal return means success; an escaping exception
     * or error means failure. Expected failures must be caught and checked within the case.
     * Runners preserve the original failure and discard observations on success. Assertions
     * must be enabled. Resources and asynchronous work must finish before this method returns.
     *
     * @param trail observations belonging exclusively to this execution
     * @throws Exception if verification or case execution fails
     */
    void run(TestTrail trail) throws Exception;
}
