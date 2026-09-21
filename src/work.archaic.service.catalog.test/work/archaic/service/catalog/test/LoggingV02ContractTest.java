package work.archaic.service.catalog.test;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import work.archaic.service.logging.v02.FailureReport;
import work.archaic.service.logging.v02.Goal;
import work.archaic.service.logging.v02.Diagnostics;
import work.archaic.service.logging.v02.Log;
import work.archaic.service.logging.v02.Observation;

/** Catalog data/default-method checks, not a substitute for provider conformance tests. */
public final class LoggingV02ContractTest {
    private static final Duration DURATION = Duration.ofMillis(10);

    public static void main(String[] args) throws Exception {
        boolean assertions = false;
        assert assertions = true;
        if (!assertions) throw new IllegalStateException("Run with -ea");
        snapshotsAreIndependent();
        invalidEvidenceIsRejected();
        notesDelegateToCurrentTrail();
        System.out.println("Logging v02 contract: 3 checks passed");
    }

    private static void snapshotsAreIndependent() {
        var observations = new ArrayList<Observation>();
        observations.add(new Observation(Duration.ZERO, "Started"));
        var failure = new IOException("Original failure");
        var report = report(observations, 2, 1, failure);
        observations.clear();
        assert report.observations().size() == 1 : "Caller mutated report contents";
        assert report.failure() == failure : "Failure identity lost";
        assert report.omittedObservations() == 2;
        assert report.truncatedObservations() == 1;
        expect(UnsupportedOperationException.class, () -> report.observations().clear());
    }

    private static void invalidEvidenceIsRejected() {
        var failure = new IOException();
        expect(NullPointerException.class, () -> new Observation(Duration.ZERO, null));
        expect(IllegalArgumentException.class,
                () -> new Observation(Duration.ofNanos(-1), "bad"));
        expect(NullPointerException.class, () -> report(null, 0, 0, failure));
        expect(NullPointerException.class, () -> report(List.of(), 0, 0, null));
        expect(IllegalArgumentException.class, () -> report(List.of(), -1, 0, failure));
        expect(IllegalArgumentException.class, () -> report(List.of(), 0, 1, failure));
        expect(IllegalArgumentException.class, () -> report(List.of(
                new Observation(Duration.ofMillis(11), "late")), 0, 0, failure));
        expect(IllegalArgumentException.class, () -> report(List.of(
                new Observation(Duration.ofMillis(2), "first"),
                new Observation(Duration.ofMillis(1), "second")), 0, 0, failure));
        expect(IllegalArgumentException.class, () -> new FailureReport(UUID.randomUUID(), " ",
                Instant.EPOCH, DURATION, List.of(), 0, 0, failure));
        expect(IllegalArgumentException.class, () -> new FailureReport(UUID.randomUUID(), "test",
                Instant.EPOCH, Duration.ofNanos(-1), List.of(), 0, 0, failure));
    }

    private static FailureReport report(List<Observation> observations, long omitted,
            long truncated, Throwable failure) {
        return new FailureReport(UUID.randomUUID(), "test", Instant.EPOCH,
                DURATION, observations, omitted, truncated, failure);
    }

    private static void notesDelegateToCurrentTrail() {
        var notes = new ArrayList<String>();
        Diagnostics diagnostics = new Diagnostics() {
            @Override public Goal goal(String name, Log log) { throw new UnsupportedOperationException(); }
            @Override public work.archaic.service.logging.v02.Trail currentTrail() {
                return notes::add;
            }
        };
        diagnostics.note("evidence");
        assert notes.equals(List.of("evidence"));
    }

    private static void expect(Class<? extends Throwable> type, Runnable operation) {
        try {
            operation.run();
        } catch (Throwable failure) {
            assert type.isInstance(failure) : "Unexpected failure: " + failure;
            return;
        }
        throw new AssertionError("Expected " + type.getName());
    }

    // Compile-time integration checks: only catalog and public JDK types cross the boundary.
    static void announceStartup(Log log) throws IOException {
        log.write("Application started; listening on port 8080");
    }

    static void announceInsideGoal(Goal goal, Log log) throws IOException {
        goal.run(() -> log.write("Configuration reloaded"));
    }

    static ExecutorService configureHttp(HttpServer server, Diagnostics provider, Log log) {
        var goal = provider.goal("http.request", log);
        var executor = goal.newExecutor();
        server.setExecutor(executor);
        server.createContext("/orders", exchange -> {
            try (exchange) {
                provider.note("Request reached handler");
                exchange.sendResponseHeaders(204, -1);
            }
        });
        return executor; // Application stops server before closing this executor.
    }

    static HttpHandler synchronousHandler(Goal goal, HttpHandler handler) {
        // IOException remains checked and fits HttpHandler.handle without an adapter exception.
        return exchange -> goal.run(() -> handler.handle(exchange));
    }
}

