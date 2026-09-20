package work.archaic.service.logging.v01;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Completed failure evidence. The observation list is defensively copied. The original throwable
 * is retained by identity, not deep-copied; recipients must treat it as read-only. This is not a
 * promise of deep immutability or a wire format.
 *
 * @param executionId identity of this execution, independent of its thread
 * @param goal stable operation name
 * @param startedAt wall-clock start for correlation
 * @param duration non-negative monotonic execution duration, excluding report publication
 * @param observations retained observations in capture order, with nondecreasing elapsed times
 * @param omittedObservations number of observations omitted completely
 * @param truncatedObservations number of retained observations whose messages were truncated
 * @param failure original escaping throwable
 */
public record FailureReport(UUID executionId, String goal, Instant startedAt, Duration duration,
        List<Observation> observations, long omittedObservations, long truncatedObservations,
        Throwable failure) {
    /** Validates metadata and copies the observations. */
    public FailureReport {
        Objects.requireNonNull(executionId, "executionId");
        Objects.requireNonNull(goal, "goal");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(duration, "duration");
        Objects.requireNonNull(failure, "failure");
        observations = List.copyOf(observations);
        if (goal.isBlank()) throw new IllegalArgumentException("Blank goal name");
        if (duration.isNegative()) throw new IllegalArgumentException("Negative duration");
        if (omittedObservations < 0 || truncatedObservations < 0
                || truncatedObservations > observations.size()) {
            throw new IllegalArgumentException("Invalid evidence loss counts");
        }
        var previous = Duration.ZERO;
        for (var observation : observations) {
            if (observation.elapsed().compareTo(previous) < 0
                    || observation.elapsed().compareTo(duration) > 0) {
                throw new IllegalArgumentException("Observation outside execution order or duration");
            }
            previous = observation.elapsed();
        }
    }
}
