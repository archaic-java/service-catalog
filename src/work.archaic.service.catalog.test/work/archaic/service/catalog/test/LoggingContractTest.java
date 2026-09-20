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
import work.archaic.service.logging.v01.FailureReport;
import work.archaic.service.logging.v01.Goal;
import work.archaic.service.logging.v01.GoalProvider;
import work.archaic.service.logging.v01.Log;
import work.archaic.service.logging.v01.Observation;

/** Catalog data/default-method checks, not a substitute for provider conformance tests. */
public final class LoggingContractTest {
    private static final Duration DURATION = Duration.ofMillis(10);

    public static void main(String[] args) throws Exception {
        boolean assertions = false;
        assert assertions = true;
        if (!assertions) throw new IllegalStateException("Run with -ea");
        snapshotsAreIndependent();
        invalidEvidenceIsRejected();
        actionDelegatesWithoutWrappingFailures();
        System.out.println("Logging contract: 3 checks passed");
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

    private static void actionDelegatesWithoutWrappingFailures() throws IOException {
        // Only exercises the catalog's default method; this is deliberately not a provider.
        var goal = new Goal() {
            int calls;
            @Override public String name() { return "test"; }
            @Override public <T, X extends Throwable> T call(Operation<T, X> work) throws X {
                calls++;
                return work.call();
            }
            @Override public ExecutorService executor() { throw new UnsupportedOperationException(); }
        };
        var ran = new boolean[1];
        goal.run(() -> ran[0] = true);
        assert ran[0] && goal.calls == 1 : "Action must run exactly once through call";
        expect(NullPointerException.class, () -> goal.run(null));
        assert goal.calls == 1 : "Null action reached execution boundary";
        var failure = new IOException("checked");
        try {
            goal.run(() -> { throw failure; });
            throw new AssertionError("Expected IOException");
        } catch (IOException actual) {
            assert actual == failure : "Checked failure was wrapped";
        }
        var error = new AssertionError("error");
        try {
            goal.run(() -> { throw error; });
            throw new AssertionError("Expected original error");
        } catch (AssertionError actual) {
            assert actual == error : "Error was wrapped";
        }
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
        log.note("Application started; listening on port 8080");
    }

    static void announceInsideGoal(Goal goal, Log log) throws IOException {
        goal.run(() -> log.note("Configuration reloaded"));
    }

    static ExecutorService configureHttp(HttpServer server, GoalProvider provider, Log log) {
        var goal = provider.goal("http.request", log);
        var executor = goal.executor();
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
