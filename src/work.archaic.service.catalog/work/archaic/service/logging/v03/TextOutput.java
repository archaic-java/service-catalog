package work.archaic.service.logging.v03;

import java.io.PrintStream;
import java.util.Objects;

/** Standard timestamped text rendering to a chosen stream.
 * Each entry and complete report synchronize on that stream, including across separate
 * renderer instances. Text fields escape backslashes, carriage returns and newlines;
 * throwable stack traces retain the JDK format. Output follows PrintStream error semantics.
 * This renderer does not close or own the stream. */
public final class TextOutput {
    private final PrintStream stream;
    public TextOutput(PrintStream stream) { this.stream = Objects.requireNonNull(stream, "stream"); }
    /** Write one timestamped entry. */
    public void entry(Entry entry) {
        Objects.requireNonNull(entry, "entry");
        synchronized (stream) { stream.println(render(entry)); }
    }
    /** Write bounded evidence, loss count, explicit reason and original throwable as one report. */
    public void failure(FailureReport report) {
        Objects.requireNonNull(report, "report");
        synchronized (stream) {
            stream.println("--- failed logging context ---");
            if (report.dropped() != 0) stream.println("[" + report.dropped() + " earlier entries dropped]");
            report.evidence().forEach(entry -> stream.println(render(entry)));
            if (report.explicitFailure() != null)
                stream.println("Failure: " + render(report.explicitFailure()));
            if (report.cause() != null) report.cause().printStackTrace(stream);
            stream.println("--- end context ---");
        }
    }
    private static String render(Entry entry) {
        return entry.timestamp() + " " + escape(entry.source()) + " " + escape(entry.message());
    }
    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n");
    }
}
