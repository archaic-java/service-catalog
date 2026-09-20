package work.archaic.service.logging.v01;

import java.io.IOException;

/**
 * Destination for immediate information and completed failure reports, without severity levels.
 * Implementations support concurrent calls and keep each message or report coherent. There is
 * no durability or automatic retry guarantee; providers document output and storage behavior.
 */
public interface Log {
    /**
     * Publishes general information immediately, independently of any active goal. This method
     * also works outside a goal and never buffers the message in a trail or conditions its
     * publication on the goal's outcome. Immediate publication does not imply durable storage.
     * Output failures are reported to the caller, not silently discarded.
     *
     * @param message information to publish, possibly empty
     * @throws IOException if the destination cannot accept the message
     * @throws NullPointerException if message is null
     */
    void note(String message) throws IOException;

    /**
     * Accepts one complete failure report. Implementations must not mutate its throwable.
     *
     * @param report completed execution evidence
     * @throws IOException if the destination cannot accept the report
     * @throws NullPointerException if report is null
     */
    void write(FailureReport report) throws IOException;
}
