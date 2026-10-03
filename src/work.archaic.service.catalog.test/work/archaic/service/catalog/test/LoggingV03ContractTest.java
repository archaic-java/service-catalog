package work.archaic.service.catalog.test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import work.archaic.service.logging.v03.Entry;
import work.archaic.service.logging.v03.FailureReport;

/** Standalone catalog data checks; provider conformance cases are reusable separately. */
public final class LoggingV03ContractTest {
    public static void main(String[] args) {
        boolean assertions = false;
        assert assertions = true;
        if (!assertions) throw new IllegalStateException("Run with -ea");
        var entry = new Entry(Instant.EPOCH, "worker", "evidence");
        var entries = new ArrayList<Entry>(); entries.add(entry);
        var cause = new java.io.IOException("failure");
        var report = new FailureReport(entries, 1, null, cause);
        entries.clear();
        assert report.evidence().equals(List.of(entry)) : "Snapshot caller-owned evidence";
        assert report.cause() == cause : "Preserve throwable identity";
        assert report.dropped() == 1 : "Preserve loss count";
        try { report.evidence().clear(); assert false : "Report evidence must be immutable"; }
        catch (UnsupportedOperationException expected) { }
        try { new FailureReport(List.of(), 0, null, null); assert false : "Require a failure reason"; }
        catch (IllegalArgumentException expected) { }
        try { new FailureReport(List.of(), -1, entry, null); assert false : "Reject negative dropped count"; }
        catch (IllegalArgumentException expected) { }
        try { new Entry(Instant.EPOCH, "worker", null); assert false : "Require a message"; }
        catch (NullPointerException expected) { }
        System.out.println("Logging v03 contract: data checks passed");
    }
}
