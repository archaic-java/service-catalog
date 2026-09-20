package work.archaic.service.logging.v01;

/**
 * Observations owned by one active execution. There are no levels, ambient attribute maps,
 * output configuration or completion methods. A trail is confined to its execution thread.
 * Providers bound retained entry count and message size and document their limits and eviction
 * policy. Dropped or truncated observations must be accounted for in the failure report.
 */
public interface Trail {
    /**
     * Captures a message now, with elapsed time from the execution start. No deferred supplier
     * or mutable application object is retained. Capacity exhaustion drops/truncates evidence
     * according to the documented policy; it does not fail the operation.
     *
     * @param message diagnostic observation
     * @throws NullPointerException if message is null
     * @throws IllegalStateException if used outside its execution or from another thread
     */
    void note(String message);
}
