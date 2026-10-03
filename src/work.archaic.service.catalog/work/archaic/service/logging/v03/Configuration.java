package work.archaic.service.logging.v03;

import java.io.PrintStream;
import java.time.Clock;
import java.util.Objects;
import java.util.function.Consumer;

/** Immutable context settings. Shared sinks must support concurrent context completion. */
public record Configuration(boolean debug, Clock clock, Consumer<Entry> entries,
                            Consumer<FailureReport> failures, int capacity, int fieldLimit) {
    public Configuration {
        Objects.requireNonNull(clock, "clock");
        Objects.requireNonNull(entries, "entries");
        Objects.requireNonNull(failures, "failures");
        if (capacity < 1 || fieldLimit < 2)
            throw new IllegalArgumentException("Capacity >= 1 and field limit >= 2 required");
    }

    /** Default retention and UTC timestamps, with explicitly selected output sinks. */
    public Configuration(boolean debug, Consumer<Entry> entries, Consumer<FailureReport> failures) {
        this(debug, Clock.systemUTC(), entries, failures, 256, 2048);
    }

    /** Standard text output to the chosen stream, UTC and default retention limits. */
    public static Configuration text(boolean debug, PrintStream stream) {
        var output = new TextOutput(stream);
        return new Configuration(debug, output::entry, output::failure);
    }
}
