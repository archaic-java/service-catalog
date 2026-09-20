package work.archaic.service.logging.v01;

import java.time.Duration;
import java.util.Objects;

/**
 * Captured evidence in a trail.
 * @param elapsed non-negative monotonic elapsed time from execution start
 * @param message captured message, possibly empty
 */
public record Observation(Duration elapsed, String message) {
    /** Validates the observation. */
    public Observation {
        Objects.requireNonNull(elapsed, "elapsed");
        Objects.requireNonNull(message, "message");
        if (elapsed.isNegative()) throw new IllegalArgumentException("Negative elapsed time");
    }
}
