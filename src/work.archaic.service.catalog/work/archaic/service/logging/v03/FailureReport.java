package work.archaic.service.logging.v03;

import java.util.List;

/** Immutable evidence snapshot; cause and explicitFailure cannot both be null. */
public record FailureReport(List<Entry> evidence, long dropped,
                            Entry explicitFailure, Throwable cause) {
    public FailureReport {
        evidence = List.copyOf(evidence);
        if (dropped < 0) throw new IllegalArgumentException("Negative dropped count");
        if (explicitFailure == null && cause == null)
            throw new IllegalArgumentException("A failure needs a reason or cause");
    }
}
