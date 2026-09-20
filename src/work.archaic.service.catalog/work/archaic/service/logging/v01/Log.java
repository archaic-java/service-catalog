package work.archaic.service.logging.v01;

import java.io.IOException;

/**
 * Destination for completed failure reports, not a per-line logger. Implementations support
 * concurrent calls and keep each report coherent. There is no durability or automatic retry
 * guarantee; providers document their output and storage behavior.
 */
@FunctionalInterface
public interface Log {
    /**
     * Accepts one complete failure report. Implementations must not mutate its throwable.
     *
     * @param report completed execution evidence
     * @throws IOException if the destination cannot accept the report
     * @throws NullPointerException if report is null
     */
    void write(FailureReport report) throws IOException;
}
