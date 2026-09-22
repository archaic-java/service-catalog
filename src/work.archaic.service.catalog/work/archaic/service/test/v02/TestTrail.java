package work.archaic.service.test.v02;

/**
 * Temporary diagnostic observations for one test execution, independent of application logging.
 *
 * <p>The trail belongs to the executing thread and is valid only while {@link TestCase#run}
 * executes. It is not inherited by spawned threads. Runners retain observations for failed
 * cases only, document retention bounds, and report any omitted or truncated observations.
 */
public interface TestTrail {
    /**
     * Records an observation in invocation order.
     *
     * @param message observation text
     * @throws NullPointerException if the message is null
     * @throws IllegalStateException if called from another thread or after the case completes
     */
    void note(String message);
}
