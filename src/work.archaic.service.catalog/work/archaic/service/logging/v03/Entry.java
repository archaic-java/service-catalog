package work.archaic.service.logging.v03;

import java.time.Instant;
import java.util.Objects;

/** Evidence captured at submission time, attributed to a logging object. */
public record Entry(Instant timestamp, String source, String message) {
    public Entry {
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(message, "message");
    }
}
